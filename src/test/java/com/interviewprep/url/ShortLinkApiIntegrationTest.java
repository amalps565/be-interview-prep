package com.interviewprep.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class ShortLinkApiIntegrationTest {

  private static final String LONG_URL = "https://example.com/articles/2026/very/long/path?id=42";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private ShortLinkRepository repository;

  @BeforeEach
  void clean() {
    repository.deleteAll();
  }

  @Test
  void shortenReturnsACodeOfAtMostEightUrlSafeCharacters() throws Exception {
    shorten("{\"url\":\"" + LONG_URL + "\"}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value(matchesRegex("[A-Za-z0-9]{7}")))
        .andExpect(jsonPath("$.shortUrl").value(startsWith("http://")))
        .andExpect(jsonPath("$.originalUrl").value(LONG_URL));
  }

  @Test
  void shorteningTheSameUrlTwiceReturnsTheSameActiveLink() throws Exception {
    String first = codeOf(shorten("{\"url\":\"" + LONG_URL + "\"}"));

    shorten("{\"url\":\"" + LONG_URL + "\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(first));
    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void aDifferentExpiryCreatesASeparateLink() throws Exception {
    String permanent = codeOf(shorten("{\"url\":\"" + LONG_URL + "\"}"));
    Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

    String expiring =
        codeOf(shorten("{\"url\":\"" + LONG_URL + "\",\"expiresAt\":\"" + tomorrow + "\"}"));

    assertThat(expiring).isNotEqualTo(permanent);
  }

  @Test
  void invalidUrlsAreRejectedWithAFieldError() throws Exception {
    for (String url : new String[] {"not a url", "ftp://example.com/file", "https://"}) {
      shorten("{\"url\":\"" + url + "\"}")
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.fieldErrors[0].field").value("url"));
    }
  }

  @Test
  void visitingTheShortUrlRedirectsAndCountsTheVisit() throws Exception {
    String code = codeOf(shorten("{\"url\":\"" + LONG_URL + "\"}"));

    mockMvc
        .perform(get("/" + code))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", LONG_URL))
        .andExpect(header().string("Cache-Control", "no-store"));
    mockMvc.perform(get("/" + code)).andExpect(status().isFound());

    mockMvc
        .perform(get("/api/urls/" + code + "/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.originalUrl").value(LONG_URL))
        .andExpect(jsonPath("$.visitCount").value(2))
        .andExpect(jsonPath("$.createdAt").isNotEmpty());
  }

  @Test
  void unknownCodeReturns404() throws Exception {
    mockMvc
        .perform(get("/nope123"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Short link nope123 not found"));
    mockMvc.perform(get("/api/urls/nope123/stats")).andExpect(status().isNotFound());
  }

  @Test
  void expiredCodeReturns410AndIsNotCounted() throws Exception {
    repository.save(new ShortLink("old1234", LONG_URL, Instant.now().minusSeconds(60)));

    mockMvc
        .perform(get("/old1234"))
        .andExpect(status().isGone())
        .andExpect(jsonPath("$.status").value(410));
    assertThat(repository.findByCode("old1234").orElseThrow().getVisitCount()).isZero();
  }

  @Test
  void expiryInThePastIsRejected() throws Exception {
    shorten("{\"url\":\"" + LONG_URL + "\",\"expiresAt\":\"2000-01-01T00:00:00Z\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("expiresAt"));
  }

  @Test
  void customCodeIsUsedAndATakenCodeReturns409() throws Exception {
    shorten("{\"url\":\"" + LONG_URL + "\",\"customCode\":\"my-link\"}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("my-link"))
        .andExpect(header().string("Location", endsWith("/my-link")));

    shorten("{\"url\":\"https://other.example.com\",\"customCode\":\"my-link\"}")
        .andExpect(status().isConflict());
    shorten("{\"url\":\"" + LONG_URL + "\",\"customCode\":\"bad code!\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("customCode"));
  }

  private ResultActions shorten(String body) throws Exception {
    return mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private String codeOf(ResultActions result) throws Exception {
    JsonNode json = objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    return json.get("code").asText();
  }
}
