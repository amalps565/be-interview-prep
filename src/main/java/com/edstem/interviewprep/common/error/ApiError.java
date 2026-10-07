package com.edstem.interviewprep.common.error;

import java.time.Instant;
import java.util.List;

public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<FieldErrorDetail> fieldErrors) {

  public static ApiError of(
      int status, String error, String message, String path, List<FieldErrorDetail> fieldErrors) {
    return new ApiError(Instant.now(), status, error, message, path, List.copyOf(fieldErrors));
  }
}
