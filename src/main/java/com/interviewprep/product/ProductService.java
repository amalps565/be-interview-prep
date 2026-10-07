package com.interviewprep.product;

import com.interviewprep.common.error.InvalidFieldException;
import com.interviewprep.common.error.ResourceNotFoundException;
import com.interviewprep.common.web.PageResponse;
import com.interviewprep.product.dto.ProductFilter;
import com.interviewprep.product.dto.ProductResponse;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

  public static final Set<String> SORTABLE_FIELDS =
      Set.of("id", "name", "category", "price", "stock", "rating", "createdAt");

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

  @Transactional(readOnly = true)
  public ProductResponse get(Long id) {
    return ProductResponse.from(find(id));
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
        throw new InvalidFieldException(
            "sort", "must be one of " + SORTABLE_FIELDS.stream().sorted().toList());
      }
    }
  }

  private Product find(Long id) {
    return productRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
  }
}
