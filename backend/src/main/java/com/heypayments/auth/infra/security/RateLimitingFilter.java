package com.heypayments.auth.infra.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heypayments.shared.domain.exception.ErrorCode;
import com.heypayments.shared.web.ProblemResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

  private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
  private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();
    String clientIp = resolveClientIp(request);

    boolean allowed = true;
    if (path.contains("/auth/login") || path.contains("/auth/verify-otp")) {
      // 5 requests per minute per IP/phone
      Bucket bucket =
          loginBuckets.computeIfAbsent(clientIp, k -> createBucket(5, Duration.ofMinutes(1)));
      allowed = bucket.tryConsume(1);
    } else if (!path.startsWith("/actuator")
        && !path.startsWith("/swagger")
        && !path.startsWith("/v3/api-docs")) {
      // 120 requests per minute general
      Bucket bucket =
          generalBuckets.computeIfAbsent(clientIp, k -> createBucket(120, Duration.ofMinutes(1)));
      allowed = bucket.tryConsume(1);
    }

    if (!allowed) {
      log.warn("Rate limit exceeded for path={} ip={}", path, clientIp);
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      ProblemResponse problem =
          new ProblemResponse(
              "urn:problem:rate-limit-exceeded",
              "Rate Limit Exceeded",
              HttpStatus.TOO_MANY_REQUESTS.value(),
              ErrorCode.RATE_LIMITED.name(),
              "Too many requests. Please try again later.",
              request.getRequestURI(),
              null,
              Collections.emptyList());
      objectMapper.writeValue(response.getOutputStream(), problem);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private Bucket createBucket(long capacity, Duration refillDuration) {
    Refill refill = Refill.greedy(capacity, refillDuration);
    Bandwidth limit = Bandwidth.classic(capacity, refill);
    return Bucket.builder().addLimit(limit).build();
  }

  private String resolveClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  public void reset() {
    loginBuckets.clear();
    generalBuckets.clear();
  }
}
