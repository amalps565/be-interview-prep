package com.edstem.interviewprep.url.dto;

import com.edstem.interviewprep.url.ShortLink;
import java.time.Instant;

public record ShortLinkStats(
    String code, String originalUrl, long visitCount, Instant createdAt, Instant expiresAt) {

  public static ShortLinkStats from(ShortLink link) {
    return new ShortLinkStats(
        link.getCode(),
        link.getOriginalUrl(),
        link.getVisitCount(),
        link.getCreatedAt(),
        link.getExpiresAt());
  }
}
