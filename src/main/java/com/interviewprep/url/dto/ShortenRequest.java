package com.interviewprep.url.dto;

import com.interviewprep.common.validation.HttpUrl;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ShortenRequest(
    @NotBlank(message = "is required")
        @Size(max = 2048, message = "must be at most 2048 characters")
        @HttpUrl
        String url,
    @Future(message = "must be in the future") Instant expiresAt,
    @Pattern(regexp = "[A-Za-z0-9_-]{3,8}", message = "must be 3 to 8 letters, digits, - or _")
        String customCode) {}
