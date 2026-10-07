package com.interviewprep.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.interviewprep.order.dto.OrderItemRequest;
import com.interviewprep.order.dto.OrderRequest;
import com.interviewprep.product.Product;
import com.interviewprep.product.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class OrderConcurrencyTest {

  @Autowired private OrderService orderService;

  @Autowired private ProductRepository productRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void fiftyBuyersRacingForTenItemsGiveExactlyTenOrders() throws Exception {
    Product product = productRepository.save(newProduct("Race Widget", 10));
    OrderRequest request = singleItem(product.getId(), 1);
    String run = UUID.randomUUID().toString();

    List<Object> outcomes =
        runAtOnce(
            50,
            i ->
                orderService
                    .place("c" + i + "-" + run + "@example.test", "key-" + i, request)
                    .order()
                    .id());

    assertThat(outcomes).filteredOn(Long.class::isInstance).hasSize(10).doesNotHaveDuplicates();
    assertThat(outcomes).filteredOn(InsufficientStockException.class::isInstance).hasSize(40);
    assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isZero();
    assertThat(ordersContaining(product.getId())).isEqualTo(10);
  }

  @Test
  void twentyIdenticalRetriesCreateOneOrderAndReserveStockOnce() throws Exception {
    Product product = productRepository.save(newProduct("Retry Widget", 10));
    OrderRequest request = singleItem(product.getId(), 3);
    String customer = "retry-" + UUID.randomUUID() + "@example.test";
    String key = UUID.randomUUID().toString();

    List<Object> outcomes =
        runAtOnce(20, i -> orderService.place(customer, key, request).order().id());

    assertThat(outcomes).hasSize(20).allMatch(Long.class::isInstance);
    assertThat(outcomes.stream().distinct()).hasSize(1);
    assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(7);
    assertThat(ordersContaining(product.getId())).isEqualTo(1);
  }

  private static Product newProduct(String name, int stock) {
    return new Product(name, "Test", new BigDecimal("2.50"), stock, 4.0);
  }

  private static OrderRequest singleItem(Long productId, int quantity) {
    return new OrderRequest(List.of(new OrderItemRequest(productId, quantity)));
  }

  private Integer ordersContaining(Long productId) {
    return jdbcTemplate.queryForObject(
        "select count(distinct order_id) from order_items where product_id = ?",
        Integer.class,
        productId);
  }

  private static List<Object> runAtOnce(int requests, IntFunction<Long> call)
      throws InterruptedException {
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Long>> futures = new ArrayList<>();
    List<Object> outcomes = new ArrayList<>();
    try (ExecutorService pool = Executors.newFixedThreadPool(requests)) {
      for (int i = 0; i < requests; i++) {
        int index = i;
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  return call.apply(index);
                }));
      }
      start.countDown();
      for (Future<Long> future : futures) {
        try {
          outcomes.add(future.get(60, TimeUnit.SECONDS));
        } catch (ExecutionException ex) {
          outcomes.add(ex.getCause());
        } catch (TimeoutException ex) {
          outcomes.add(ex);
        }
      }
    }
    return outcomes;
  }
}
