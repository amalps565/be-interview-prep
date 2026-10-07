package com.interviewprep.auth.dto;

import com.interviewprep.auth.AppUser;
import com.interviewprep.auth.Role;
import java.time.Instant;

public record UserResponse(Long id, String email, Role role, Instant createdAt) {

  public static UserResponse from(AppUser user) {
    return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
  }
}
