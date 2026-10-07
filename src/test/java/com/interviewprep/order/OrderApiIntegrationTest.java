package com.interviewprep.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.product.Product;
import com.interviewprep.product.ProductRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "buyer@example.test")
class OrderApiIntegrationTest {

  private static final String OTHER_CUSTOMER = "someone-else@example.test";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private ProductRepository productRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void placingAnOrderReturns201WithLocationTotalAndItems() throws Exception {
    Product product = productRepository.save(newProduct("Mug", "4.25", 10));

    ResultActions result = place(newKey(), lines(product.getId(), 2));

    result
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString("/api/orders/")))
        .andExpect(jsonPath("$.status").value("PLACED"))
        .andExpect(jsonPath("$.total").value(8.50))
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].productId").value(product.getId()))
        .andExpect(jsonPath("$.items[0].productName").value("Mug"))
        .andExpect(jsonPath("$.items[0].quantity").value(2))
        .andExpect(jsonPath("$.items[0].lineTotal").value(8.50));
    assertThat(stockOf(product)).isEqualTo(8);
  }

  @Test
  void retryWithTheSameKeyAndBodyReplaysTheOrderWithoutReservingAgain() throws Exception {
    Product product = productRepository.save(newProduct("Pen", "1.00", 10));
    String key = newKey();
    String body = lines(product.getId(), 3);
    long firstId = idOf(place(key, body).andExpect(status().isCreated()));

    ResultActions retry = place(key, body);

    retry
        .andExpect(status().isOk())
        .andExpect(header().string(OrderController.REPLAYED_HEADER, "true"))
        .andExpect(jsonPath("$.id").value(firstId));
    assertThat(stockOf(product)).isEqualTo(7);
    assertThat(ordersContaining(product)).isEqualTo(1);
  }

  @Test
  void reusingAKeyWithADifferentBodyReturns422() throws Exception {
    Product product = productRepository.save(newProduct("Cup", "2.00", 10));
    String key = newKey();
    place(key, lines(product.getId(), 1)).andExpect(status().isCreated());

    ResultActions reuse = place(key, lines(product.getId(), 2));

    reuse.andExpect(status().isUnprocessableEntity());
    assertThat(stockOf(product)).isEqualTo(9);
  }

  @Test
  void insufficientStockOnOneLineRejectsTheWholeOrder() throws Exception {
    Product plenty = productRepository.save(newProduct("Plenty", "1.00", 5));
    Product scarce = productRepository.save(newProduct("Scarce", "1.00", 1));
    String body =
        "{\"items\":[{\"productId\":"
            + plenty.getId()
            + ",\"quantity\":2},{\"productId\":"
            + scarce.getId()
            + ",\"quantity\":2}]}";

    ResultActions result = place(newKey(), body);

    result
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value(containsString("Scarce")))
        .andExpect(jsonPath("$.message").value(containsString(String.valueOf(scarce.getId()))));
    assertThat(stockOf(plenty)).isEqualTo(5);
    assertThat(stockOf(scarce)).isEqualTo(1);
    assertThat(ordersContaining(plenty)).isZero();
  }

  @Test
  void cancellingRestoresStockAndASecondCancelReturns409() throws Exception {
    Product first = productRepository.save(newProduct("First", "1.00", 5));
    Product second = productRepository.save(newProduct("Second", "1.00", 5));
    String body =
        "{\"items\":[{\"productId\":"
            + first.getId()
            + ",\"quantity\":2},{\"productId\":"
            + second.getId()
            + ",\"quantity\":3}]}";
    long id = idOf(place(newKey(), body).andExpect(status().isCreated()));

    ResultActions cancel = mockMvc.perform(post("/api/orders/" + id + "/cancel"));

    cancel.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    assertThat(stockOf(first)).isEqualTo(5);
    assertThat(stockOf(second)).isEqualTo(5);
    mockMvc.perform(post("/api/orders/" + id + "/cancel")).andExpect(status().isConflict());
    assertThat(stockOf(first)).isEqualTo(5);
    mockMvc
        .perform(get("/api/orders/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  void anotherCustomerCannotSeeOrCancelTheOrder() throws Exception {
    Product product = productRepository.save(newProduct("Private", "1.00", 5));
    long id = idOf(place(newKey(), lines(product.getId(), 1)).andExpect(status().isCreated()));

    ResultActions read = mockMvc.perform(get("/api/orders/" + id).with(asOtherCustomer()));
    ResultActions cancel =
        mockMvc.perform(post("/api/orders/" + id + "/cancel").with(asOtherCustomer()));

    read.andExpect(status().isNotFound());
    cancel.andExpect(status().isNotFound());
    assertThat(stockOf(product)).isEqualTo(4);
  }

  @Test
  void missingOrBlankIdempotencyKeyReturns400() throws Exception {
    Product product = productRepository.save(newProduct("Keyless", "1.00", 5));
    String body = lines(product.getId(), 1);

    ResultActions missing =
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body));
    ResultActions blank = place("   ", body);

    missing.andExpect(status().isBadRequest());
    blank
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value(OrderService.IDEMPOTENCY_KEY));
    assertThat(stockOf(product)).isEqualTo(5);
  }

  @Test
  void emptyItemsReturns400WithFieldErrors() throws Exception {
    ResultActions result = place(newKey(), "{\"items\":[]}");

    result
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("items"));
  }

  @Test
  void zeroQuantityReturns400() throws Exception {
    Product product = productRepository.save(newProduct("Zero", "1.00", 5));

    ResultActions result = place(newKey(), lines(product.getId(), 0));

    result
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("items[0].quantity"));
    assertThat(stockOf(product)).isEqualTo(5);
  }

  @Test
  void unknownProductReturns404() throws Exception {
    ResultActions result = place(newKey(), lines(Long.MAX_VALUE, 1));

    result.andExpect(status().isNotFound());
  }

  @Test
  void productReadAfterAnOrderShowsTheReducedStock() throws Exception {
    Product product = productRepository.save(newProduct("Cached", "1.00", 6));
    mockMvc
        .perform(get("/api/products/" + product.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stock").value(6));
    long id = idOf(place(newKey(), lines(product.getId(), 4)).andExpect(status().isCreated()));

    ResultActions afterOrder = mockMvc.perform(get("/api/products/" + product.getId()));

    afterOrder.andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(2));
    mockMvc.perform(post("/api/orders/" + id + "/cancel")).andExpect(status().isOk());
    mockMvc
        .perform(get("/api/products/" + product.getId()))
        .andExpect(jsonPath("$.stock").value(6));
  }

  @Test
  @WithAnonymousUser
  void requestWithoutLoginReturns401() throws Exception {
    ResultActions result = place(newKey(), lines(1L, 1));

    result.andExpect(status().isUnauthorized());
  }

  private ResultActions place(String key, String body) throws Exception {
    return mockMvc.perform(
        post("/api/orders")
            .header(OrderService.IDEMPOTENCY_KEY, key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private static RequestPostProcessor asOtherCustomer() {
    return user(OTHER_CUSTOMER);
  }

  private static Product newProduct(String name, String price, int stock) {
    return new Product(name, "Test", new BigDecimal(price), stock, 4.0);
  }

  private static String lines(Long productId, int quantity) {
    return "{\"items\":[{\"productId\":" + productId + ",\"quantity\":" + quantity + "}]}";
  }

  private static String newKey() {
    return UUID.randomUUID().toString();
  }

  private int stockOf(Product product) {
    return productRepository.findById(product.getId()).orElseThrow().getStock();
  }

  private Integer ordersContaining(Product product) {
    return jdbcTemplate.queryForObject(
        "select count(distinct order_id) from order_items where product_id = ?",
        Integer.class,
        product.getId());
  }

  private long idOf(ResultActions result) throws Exception {
    JsonNode json = objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    return json.get("id").asLong();
  }
}
