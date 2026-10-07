package com.interviewprep.url;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {

  @Test
  void codesAreSevenUrlSafeCharactersAndDoNotRepeat() {
    ShortCodeGenerator generator = new ShortCodeGenerator();
    Set<String> codes = new HashSet<>();

    for (int i = 0; i < 10_000; i++) {
      String code = generator.next();
      assertThat(code).matches("[A-Za-z0-9]{7}");
      codes.add(code);
    }

    assertThat(codes).hasSize(10_000);
  }
}
