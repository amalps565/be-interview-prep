package com.interviewprep.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.common.error.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public JsonSecurityErrorHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setHeader("WWW-Authenticate", "Bearer");
    write(response, request, HttpStatus.UNAUTHORIZED, "Authentication is required");
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    write(response, request, HttpStatus.FORBIDDEN, "You do not have permission for this action");
  }

  private void write(
      HttpServletResponse response, HttpServletRequest request, HttpStatus status, String message)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    ApiError error =
        ApiError.of(
            status.value(), status.getReasonPhrase(), message, request.getRequestURI(), List.of());
    objectMapper.writeValue(response.getOutputStream(), error);
  }
}
