package com.edstem.interviewprep.url;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

final class DedupKeys {

  private static final String NO_EXPIRY = "never";

  private DedupKeys() {}

  static String of(String url, Instant expiresAt) {
    String source = url + "|" + (expiresAt == null ? NO_EXPIRY : expiresAt.toString());
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }
}
