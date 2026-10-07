package com.interviewprep.order;

import com.interviewprep.common.error.ApiException;
import com.interviewprep.common.error.InvalidFieldException;
import com.interviewprep.common.error.ResourceNotFoundException;
import com.interviewprep.order.dto.OrderItemRequest;
import com.interviewprep.order.dto.OrderRequest;
import com.interviewprep.order.dto.OrderResponse;
import com.interviewprep.product.Product;
import com.interviewprep.product.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderService {

  static final String IDEMPOTENCY_KEY = "Idempotency-Key";
  static final String PRODUCT_CACHE = "products";
  private static final int MAX_KEY_LENGTH = 100;

  private final CustomerOrderRepository orders;
  private final StockRepository stock;
  private final ProductRepository products;
  private final TransactionTemplate transaction;
  private final CacheManager cacheManager;

  public OrderService(
      CustomerOrderRepository orders,
      StockRepository stock,
      ProductRepository products,
      TransactionTemplate transaction,
      CacheManager cacheManager) {
    this.orders = orders;
    this.stock = stock;
    this.products = products;
    this.transaction = transaction;
    this.cacheManager = cacheManager;
  }

  public PlacedOrder place(String customer, String idempotencyKey, OrderRequest request) {
    String key = validKey(idempotencyKey);
    Map<Long, Integer> lines = mergeLines(request.items());
    String requestHash = hash(lines);
    Optional<CustomerOrder> existing = orders.findByCustomerEmailAndIdempotencyKey(customer, key);
    if (existing.isPresent()) {
      return replay(existing.get(), requestHash);
    }
    try {
      OrderResponse created =
          transaction.execute(status -> create(customer, key, requestHash, lines));
      return new PlacedOrder(created, false);
    } catch (DataIntegrityViolationException concurrentRetry) {
      CustomerOrder winner =
          orders
              .findByCustomerEmailAndIdempotencyKey(customer, key)
              .orElseThrow(() -> concurrentRetry);
      return replay(winner, requestHash);
    }
  }

  @Transactional(readOnly = true)
  public OrderResponse get(String customer, Long id) {
    return OrderResponse.from(findOwned(customer, id));
  }

  public OrderResponse cancel(String customer, Long id) {
    return transaction.execute(
        status -> {
          CustomerOrder order = findOwned(customer, id);
          List<OrderItem> items = order.getItems();
          if (orders.cancelIfPlaced(id) == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Order " + id + " is already cancelled");
          }
          for (OrderItem item : items) {
            stock.release(item.getProductId(), item.getQuantity());
          }
          evictProducts(items.stream().map(OrderItem::getProductId).toList());
          return OrderResponse.from(findOwned(customer, id));
        });
  }

  private OrderResponse create(
      String customer, String key, String requestHash, Map<Long, Integer> lines) {
    CustomerOrder order = new CustomerOrder(customer, key, requestHash);
    for (Map.Entry<Long, Integer> line : lines.entrySet()) {
      Long productId = line.getKey();
      int quantity = line.getValue();
      Product product =
          products
              .findById(productId)
              .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
      if (stock.reserve(productId, quantity) == 0) {
        throw new InsufficientStockException(product.getName(), productId, quantity);
      }
      order.addItem(productId, product.getName(), product.getPrice(), quantity);
    }
    CustomerOrder saved = orders.saveAndFlush(order);
    evictProducts(lines.keySet());
    return OrderResponse.from(saved);
  }

  private PlacedOrder replay(CustomerOrder order, String requestHash) {
    if (!order.getRequestHash().equals(requestHash)) {
      throw new ApiException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          IDEMPOTENCY_KEY + " was already used for a different order");
    }
    return new PlacedOrder(OrderResponse.from(order), true);
  }

  private CustomerOrder findOwned(String customer, Long id) {
    return orders
        .findByIdAndCustomerEmail(id, customer)
        .orElseThrow(() -> new ResourceNotFoundException("Order", id));
  }

  private void evictProducts(Collection<Long> productIds) {
    Cache cache = cacheManager.getCache(PRODUCT_CACHE);
    if (cache != null) {
      productIds.forEach(cache::evict);
    }
  }

  private static String validKey(String key) {
    String trimmed = key == null ? "" : key.strip();
    if (trimmed.isEmpty() || trimmed.length() > MAX_KEY_LENGTH) {
      throw new InvalidFieldException(IDEMPOTENCY_KEY, "must be 1 to 100 characters");
    }
    return trimmed;
  }

  private static Map<Long, Integer> mergeLines(List<OrderItemRequest> items) {
    return items.stream()
        .collect(
            Collectors.toMap(
                OrderItemRequest::productId,
                OrderItemRequest::quantity,
                Integer::sum,
                TreeMap::new));
  }

  private static String hash(Map<Long, Integer> lines) {
    String canonical =
        lines.entrySet().stream()
            .map(line -> line.getKey() + ":" + line.getValue())
            .collect(Collectors.joining(";"));
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }
}
