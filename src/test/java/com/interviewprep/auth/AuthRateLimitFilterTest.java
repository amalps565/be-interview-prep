package com.interviewprep.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthRateLimitFilterTest {

  @Test
  void trackedClientsStayBoundedWhenAddressesRotate() throws Exception {
    AuthRateLimitFilter filter =
        new AuthRateLimitFilter(
            3,
            Duration.ofMinutes(1),
            new StaticListableBeanFactory().getBeanProvider(Clock.class),
            new ObjectMapper());

    for (int client = 0; client < 25_000; client++) {
      MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
      request.setRemoteAddr(
          "10." + (client / 65_536) + "." + (client / 256 % 256) + "." + client % 256);
      filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    assertThat(filter.trackedClients()).isLessThanOrEqualTo(10_000);
  }
}
