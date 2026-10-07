package com.interviewprep.product;

import com.interviewprep.common.web.PageResponse;
import com.interviewprep.product.dto.ProductFilter;
import com.interviewprep.product.dto.ProductResponse;
import java.math.BigDecimal;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @GetMapping
  public PageResponse<ProductResponse> list(
      @RequestParam(required = false) String category,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(required = false) Boolean inStock,
      @RequestParam(required = false) String name,
      @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC)
          Pageable pageable) {
    ProductFilter filter = new ProductFilter(category, minPrice, maxPrice, inStock, name);
    return productService.list(filter, pageable);
  }

  @GetMapping("/{id}")
  public ProductResponse get(@PathVariable Long id) {
    return productService.get(id);
  }
}
