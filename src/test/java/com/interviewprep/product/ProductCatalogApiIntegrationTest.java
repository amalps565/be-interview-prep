package com.interviewprep.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class ProductCatalogApiIntegrationTest {

  private static final String VALID_BODY =
      "{\"name\":\"Desk Lamp\",\"category\":\"Home\",\"price\":19.99,\"stock\":5,\"rating\":4.5}";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private ProductRepository productRepository;

  @Test
  void seedCreatesOneHundredProductsAndReportsPageCounts() throws Exception {
    mockMvc
        .perform(get("/api/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(ProductSeeder.PRODUCT_COUNT))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.totalPages").value(5))
        .andExpect(jsonPath("$.content.length()").value(20))
        .andExpect(jsonPath("$.content[0].id").value(firstId()));

    mockMvc
        .perform(get("/api/products").param("size", "30").param("page", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalPages").value(4))
        .andExpect(jsonPath("$.page").value(3))
        .andExpect(jsonPath("$.content.length()").value(10));
  }

  @Test
  void categoryFilterIsCaseInsensitive() throws Exception {
    String category = sample().getCategory();
    assertMatches(
        get("/api/products").param("category", category.toUpperCase(Locale.ROOT)),
        product -> product.getCategory().equalsIgnoreCase(category));
  }

  @Test
  void priceRangeFilterIsInclusive() throws Exception {
    assertMatches(
        get("/api/products").param("minPrice", "100.00").param("maxPrice", "500.00"),
        product ->
            product.getPrice().compareTo(new BigDecimal("100.00")) >= 0
                && product.getPrice().compareTo(new BigDecimal("500.00")) <= 0);
    assertMatches(
        get("/api/products").param("minPrice", "900"),
        product -> product.getPrice().compareTo(new BigDecimal("900")) >= 0);
    assertMatches(
        get("/api/products").param("maxPrice", "50"),
        product -> product.getPrice().compareTo(new BigDecimal("50")) <= 0);
  }

  @Test
  void inStockFilterExcludesSoldOutProducts() throws Exception {
    assertThat(productRepository.findAll()).anyMatch(product -> product.getStock() == 0);
    assertMatches(get("/api/products").param("inStock", "true"), product -> product.getStock() > 0);
  }

  @Test
  void nameFilterMatchesAnyPartIgnoringCase() throws Exception {
    assertMatches(
        get("/api/products").param("name", "LAMP"),
        product -> product.getName().toLowerCase(Locale.ROOT).contains("lamp"));
  }

  @Test
  void allFiltersCombineInOneRequest() throws Exception {
    Product target = sample();
    String noun = target.getName().split(" ")[1];
    BigDecimal minPrice = target.getPrice().subtract(new BigDecimal("200")).max(BigDecimal.ZERO);
    BigDecimal maxPrice = target.getPrice().add(new BigDecimal("200"));
    List<Long> ids =
        assertMatches(
            get("/api/products")
                .param("category", target.getCategory().toLowerCase(Locale.ROOT))
                .param("minPrice", minPrice.toPlainString())
                .param("maxPrice", maxPrice.toPlainString())
                .param("inStock", "true")
                .param("name", noun.toLowerCase(Locale.ROOT)),
            product ->
                product.getCategory().equals(target.getCategory())
                    && product.getPrice().compareTo(minPrice) >= 0
                    && product.getPrice().compareTo(maxPrice) <= 0
                    && product.getStock() > 0
                    && product.getName().contains(noun));
    assertThat(ids).contains(target.getId());
  }

  @Test
  void sortsByPriceDescending() throws Exception {
    JsonNode content = fetch(get("/api/products").param("sort", "price,desc").param("size", "100"));
    List<BigDecimal> prices = new ArrayList<>();
    content.forEach(item -> prices.add(item.get("price").decimalValue()));
    assertThat(prices).hasSize(ProductSeeder.PRODUCT_COUNT);
    assertThat(prices).isSortedAccordingTo(Comparator.reverseOrder());
  }

  @Test
  void rejectsSortByUnknownField() throws Exception {
    mockMvc
        .perform(get("/api/products").param("sort", "password,asc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("sort"));
  }

  @Test
  void capsPageSizeAtOneHundred() throws Exception {
    mockMvc
        .perform(get("/api/products").param("size", "500"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(100))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.content.length()").value(ProductSeeder.PRODUCT_COUNT));
  }

  @Test
  void rejectsMinPriceAboveMaxPrice() throws Exception {
    mockMvc
        .perform(get("/api/products").param("minPrice", "500").param("maxPrice", "100"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("minPrice"));
  }

  @Test
  void returnsNotFoundForUnknownProduct() throws Exception {
    mockMvc.perform(get("/api/products/999999")).andExpect(status().isNotFound());
  }

  @Test
  void userCannotChangeProducts() throws Exception {
    long id = firstId();
    mockMvc
        .perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            put("/api/products/" + id).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isForbidden());
    mockMvc.perform(delete("/api/products/" + id)).andExpect(status().isForbidden());
    assertThat(productRepository.count()).isEqualTo(ProductSeeder.PRODUCT_COUNT);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminGetsFieldErrorsForInvalidProduct() throws Exception {
    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"\",\"category\":\"Home\",\"price\":-1,\"stock\":1,\"rating\":6}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.length()").value(3));
  }

  private List<Long> assertMatches(MockHttpServletRequestBuilder request, Predicate<Product> rule)
      throws Exception {
    request.param("size", "100");
    List<Long> expected =
        productRepository.findAll().stream().filter(rule).map(Product::getId).sorted().toList();
    List<Long> actual = new ArrayList<>();
    fetch(request).forEach(item -> actual.add(item.get("id").asLong()));
    assertThat(expected).isNotEmpty();
    assertThat(actual).isEqualTo(expected);
    return actual;
  }

  private JsonNode fetch(MockHttpServletRequestBuilder request) throws Exception {
    String body =
        mockMvc
            .perform(request)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body).get("content");
  }

  private Product sample() {
    return productRepository.findAll().stream()
        .filter(product -> product.getStock() > 0)
        .min(Comparator.comparing(Product::getId))
        .orElseThrow();
  }

  private long firstId() {
    return productRepository.findAll().stream().mapToLong(Product::getId).min().orElseThrow();
  }
}
