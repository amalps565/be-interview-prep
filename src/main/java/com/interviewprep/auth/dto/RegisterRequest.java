package com.interviewprep.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "is required")
        @Email(message = "must be a valid email address")
        @Size(max = 254, message = "must be at most 254 characters")
        String email,
    @NotBlank(message = "is required")
        @Size(min = 8, max = 72, message = "must be 8 to 72 characters")
        String password) {}
