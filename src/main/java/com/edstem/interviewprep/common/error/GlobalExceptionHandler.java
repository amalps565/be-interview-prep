package com.edstem.interviewprep.common.error;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<FieldErrorDetail> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
            .toList();
    return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    if (ex.getCause() instanceof MismatchedInputException mismatch
        && !mismatch.getPath().isEmpty()) {
      String field =
          mismatch.getPath().stream()
              .map(reference -> reference.getFieldName())
              .collect(Collectors.joining("."));
      String message =
          mismatch instanceof InvalidFormatException invalid
                  && invalid.getTargetType() != null
                  && invalid.getTargetType().isEnum()
              ? "must be one of " + List.of(invalid.getTargetType().getEnumConstants())
              : "has an invalid value";
      return build(
          HttpStatus.BAD_REQUEST,
          "Validation failed",
          request,
          List.of(new FieldErrorDetail(field, message)));
    }
    return build(
        HttpStatus.BAD_REQUEST, "Request body is missing or malformed", request, List.of());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    Class<?> type = ex.getRequiredType();
    String message =
        type != null && type.isEnum()
            ? "must be one of " + List.of(type.getEnumConstants())
            : "has an invalid value";
    return build(
        HttpStatus.BAD_REQUEST,
        "Validation failed",
        request,
        List.of(new FieldErrorDetail(ex.getName(), message)));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(
      ResourceNotFoundException ex, HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest request) {
    return build(ex.getStatus(), ex.getMessage(), request, List.of());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
    if (ex instanceof ErrorResponse errorResponse) {
      String detail = errorResponse.getBody().getDetail();
      return build(
          errorResponse.getStatusCode(),
          detail != null ? detail : ex.getMessage(),
          request,
          List.of());
    }
    log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, List.of());
  }

  private ResponseEntity<ApiError> build(
      HttpStatusCode status,
      String message,
      HttpServletRequest request,
      List<FieldErrorDetail> fieldErrors) {
    HttpStatus resolved = HttpStatus.resolve(status.value());
    String error = resolved != null ? resolved.getReasonPhrase() : String.valueOf(status.value());
    return ResponseEntity.status(status)
        .body(ApiError.of(status.value(), error, message, request.getRequestURI(), fieldErrors));
  }
}
