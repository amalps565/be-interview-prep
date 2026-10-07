package com.interviewprep.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.common.error.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

  private static final String AUTH_PATH = "/api/auth/";
  private static final int MAX_TRACKED_CLIENTS = 10_000;

  private final int limit;
  private final Duration window;
  private final Clock clock;
  private final ObjectMapper objectMapper;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public AuthRateLimitFilter(
      @Value("${app.auth.rate-limit.requests}") int limit,
      @Value("${app.auth.rate-limit.window}") Duration window,
      ObjectProvider<Clock> clock,
      ObjectMapper objectMapper) {
    this.limit = limit;
    this.window = window;
    this.clock = clock.getIfAvailable(Clock::systemUTC);
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith(AUTH_PATH);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Instant now = Instant.now(clock);
    Window current =
        windows.compute(
            request.getRemoteAddr(),
            (client, previous) ->
                previous == null || previous.endsBefore(now, window)
                    ? new Window(now, 1)
                    : new Window(previous.start(), previous.count() + 1));
    evictExpired(now);
    if (current.count() > limit) {
      reject(request, response, current, now);
      return;
    }
    chain.doFilter(request, response);
  }

  private void evictExpired(Instant now) {
    if (windows.size() > MAX_TRACKED_CLIENTS) {
      windows.values().removeIf(entry -> entry.endsBefore(now, window));
    }
  }

  private void reject(
      HttpServletRequest request, HttpServletResponse response, Window current, Instant now)
      throws IOException {
    long retryAfter = Math.max(1, Duration.between(now, current.start().plus(window)).toSeconds());
    HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
    response.setStatus(status.value());
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    ApiError error =
        ApiError.of(
            status.value(),
            status.getReasonPhrase(),
            "Too many login or registration attempts; try again in " + retryAfter + " seconds",
            request.getRequestURI(),
            List.of());
    objectMapper.writeValue(response.getOutputStream(), error);
  }

  private record Window(Instant start, int count) {

    boolean endsBefore(Instant now, Duration length) {
      return !now.isBefore(start.plus(length));
    }
  }
}
