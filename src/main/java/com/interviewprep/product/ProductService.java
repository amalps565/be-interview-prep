package com.interviewprep.product;

import com.interviewprep.common.error.InvalidFieldException;
import com.interviewprep.common.error.ResourceNotFoundException;
import com.interviewprep.common.web.PageResponse;
import com.interviewprep.product.dto.ProductFilter;
import com.interviewprep.product.dto.ProductRequest;
import com.interviewprep.product.dto.ProductResponse;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

  public static final String CACHE_NAME = "products";

  private static final List<String> SORTABLE_FIELDS =
      List.of("id", "name", "category", "price", "stock", "rating", "createdAt");

  private static final String RESOURCE = "Product";

  private final ProductRepository productRepository;

  public ProductService(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  @Transactional(readOnly = true)
  public PageResponse<ProductResponse> list(ProductFilter filter, Pageable pageable) {
    rejectInvertedPriceRange(filter);
    rejectUnknownSortFields(pageable.getSort());
    return PageResponse.from(
        productRepository
            .findAll(ProductSpecifications.matching(filter), pageable)
            .map(ProductResponse::from));
  }

  @Cacheable(cacheNames = CACHE_NAME, key = "#id")
  @Transactional(readOnly = true)
  public ProductResponse get(Long id) {
    return ProductResponse.from(find(id));
  }

  @Transactional
  public ProductResponse create(ProductRequest request) {
    Product product =
        new Product(
            request.name().strip(),
            request.category().strip(),
            request.price(),
            request.stock(),
            request.rating());
    return ProductResponse.from(productRepository.save(product));
  }

  @CachePut(cacheNames = CACHE_NAME, key = "#id")
  @Transactional
  public ProductResponse update(Long id, ProductRequest request) {
    Product product = find(id);
    product.update(
        request.name().strip(),
        request.category().strip(),
        request.price(),
        request.stock(),
        request.rating());
    return ProductResponse.from(product);
  }

  @CacheEvict(cacheNames = CACHE_NAME, key = "#id")
  @Transactional
  public void delete(Long id) {
    productRepository.delete(find(id));
  }

  private void rejectInvertedPriceRange(ProductFilter filter) {
    if (filter.minPrice() != null
        && filter.maxPrice() != null
        && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
      throw new InvalidFieldException("minPrice", "must not be greater than maxPrice");
    }
  }

  private void rejectUnknownSortFields(Sort sort) {
    for (Sort.Order order : sort) {
      if (!SORTABLE_FIELDS.contains(order.getProperty())) {
        throw new InvalidFieldException("sort", "must be one of " + SORTABLE_FIELDS);
      }
    }
  }

  private Product find(Long id) {
    return productRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
  }
}
