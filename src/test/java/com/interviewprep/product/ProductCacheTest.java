package com.interviewprep.product;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class ProductCacheTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CacheManager cacheManager;

  @MockitoSpyBean private ProductRepository repository;

  @BeforeEach
  void clearCache() {
    cacheManager.getCache(ProductService.CACHE_NAME).clear();
    clearInvocations(repository);
  }

  @Test
  void repeatedLookupsHitTheDatabaseOnce() throws Exception {
    long id =
        create(
            "{\"name\":\"Cached Kettle\",\"category\":\"Home\",\"price\":25.00,"
                + "\"stock\":3,\"rating\":4.0}");
    clearInvocations(repository);

    mockMvc.perform(get("/api/products/" + id)).andExpect(status().isOk());
    mockMvc
        .perform(get("/api/products/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Cached Kettle"));

    verify(repository, times(1)).findById(id);
    mockMvc.perform(delete("/api/products/" + id)).andExpect(status().isNoContent());
  }

  @Test
  void updateAndDeleteNeverLeaveStaleEntries() throws Exception {
    long id =
        create(
            "{\"name\":\"Stale Lamp\",\"category\":\"Home\",\"price\":10.00,"
                + "\"stock\":3,\"rating\":4.0}");
    mockMvc.perform(get("/api/products/" + id)).andExpect(jsonPath("$.price").value(10.00));

    mockMvc
        .perform(
            put("/api/products/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Stale Lamp\",\"category\":\"Home\",\"price\":12.50,"
                        + "\"stock\":3,\"rating\":4.0}"))
        .andExpect(status().isOk());
    clearInvocations(repository);

    mockMvc
        .perform(get("/api/products/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.price").value(12.50));
    verify(repository, never()).findById(anyLong());

    mockMvc.perform(delete("/api/products/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
  }

  private long create(String body) throws Exception {
    String response =
        mockMvc
            .perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("id").asLong();
  }
}
