package com.edstem.interviewprep.url.dto;

import java.time.Instant;

public record ShortLinkResponse(
    String code, String shortUrl, String originalUrl, Instant createdAt, Instant expiresAt) {}
