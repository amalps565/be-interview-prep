package com.interviewprep.product;

import com.interviewprep.product.dto.ProductFilter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

  private static final char LIKE_ESCAPE = '\\';

  private ProductSpecifications() {}

  public static Specification<Product> matching(ProductFilter filter) {
    List<Specification<Product>> specs = new ArrayList<>();
    if (hasText(filter.category())) {
      specs.add(categoryEquals(filter.category()));
    }
    if (filter.minPrice() != null) {
      specs.add(priceAtLeast(filter.minPrice()));
    }
    if (filter.maxPrice() != null) {
      specs.add(priceAtMost(filter.maxPrice()));
    }
    if (Boolean.TRUE.equals(filter.inStock())) {
      specs.add(inStock());
    }
    if (hasText(filter.name())) {
      specs.add(nameContains(filter.name()));
    }
    return Specification.allOf(specs);
  }

  public static Specification<Product> categoryEquals(String category) {
    String value = category.strip().toLowerCase(Locale.ROOT);
    return (root, query, cb) -> cb.equal(cb.lower(root.get("category")), value);
  }

  public static Specification<Product> priceAtLeast(BigDecimal minPrice) {
    return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
  }

  public static Specification<Product> priceAtMost(BigDecimal maxPrice) {
    return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
  }

  public static Specification<Product> inStock() {
    return (root, query, cb) -> cb.greaterThan(root.get("stock"), 0);
  }

  public static Specification<Product> nameContains(String name) {
    String pattern = "%" + escapeLike(name.strip().toLowerCase(Locale.ROOT)) + "%";
    return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE);
  }

  private static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
