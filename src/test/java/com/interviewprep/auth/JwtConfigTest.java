package com.interviewprep.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtConfigTest {

  @Test
  void missingOrShortSecretStopsStartup() {
    for (String secret : new String[] {null, "", "too-short"}) {
      JwtProperties properties = new JwtProperties(secret, Duration.ofMinutes(15));

      assertThatThrownBy(() -> new JwtConfig(properties))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("JWT_SECRET");
    }
  }
}
