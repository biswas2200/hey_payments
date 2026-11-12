package com.heypayments.shared.infra.security;

import com.heypayments.shared.domain.exception.ErrorCode;
import com.heypayments.shared.web.ProblemResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Authentication entry point writing RFC 9457 Problem Details for unauthenticated requests.
 * Replaces HTTP Basic challenge header per docs/reviews/REVIEW-phase-0.md MUST-FIX 2.
 */
@Component
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final JsonMapper jsonMapper;

  public ProblemAuthenticationEntryPoint(ObjectProvider<JsonMapper> jsonMapperProvider) {
    this.jsonMapper = jsonMapperProvider.getIfAvailable(JsonMapper::new);
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    String requestId = MDC.get("requestId");
    if (requestId == null || requestId.isBlank()) {
      requestId = request.getHeader("X-Request-Id");
      if (requestId == null || requestId.isBlank()) {
        requestId = UUID.randomUUID().toString();
      }
    }

    ErrorCode code = ErrorCode.UNAUTHENTICATED;
    response.setStatus(code.getStatusCode());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    final ProblemResponse body =
        new ProblemResponse(
            code.getTypeUri(),
            code.getTitle(),
            code.getStatusCode(),
            code.name(),
            "Authentication is required to access this resource",
            request.getRequestURI(),
            requestId,
            null);
    jsonMapper.writeValue(response.getOutputStream(), body);
  }
}
