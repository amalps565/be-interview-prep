package com.interviewprep.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = "app.auth.rate-limit.requests=3")
@AutoConfigureMockMvc
class AuthRateLimitTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void loginAttemptsBeyondTheLimitGet429UntilTheWindowEnds() throws Exception {
    for (int attempt = 0; attempt < 3; attempt++) {
      mockMvc.perform(login("10.0.0.1")).andExpect(status().isUnauthorized());
    }

    mockMvc
        .perform(login("10.0.0.1"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(jsonPath("$.status").value(429));
    mockMvc.perform(login("10.0.0.2")).andExpect(status().isUnauthorized());
  }

  @Test
  void otherEndpointsAreNotLimited() throws Exception {
    for (int attempt = 0; attempt < 5; attempt++) {
      mockMvc
          .perform(get("/api/tasks").with(remote("10.0.0.3")))
          .andExpect(status().isUnauthorized());
    }
  }

  private MockHttpServletRequestBuilder login(String clientAddress) {
    return post("/api/auth/login")
        .with(remote(clientAddress))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"nobody@example.test\",\"password\":\"wrong-guess\"}");
  }

  private RequestPostProcessor remote(String address) {
    return request -> {
      request.setRemoteAddr(address);
      return request;
    };
  }
}
