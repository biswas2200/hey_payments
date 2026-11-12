package com.heypayments.shared.web;

import com.heypayments.shared.infra.id.UuidV7Generator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that attaches a unique X-Request-Id to every HTTP request and response, and sets it in the
 * logging MDC context for structured tracing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  public static final String MDC_KEY = "requestId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String requestId = request.getHeader(REQUEST_ID_HEADER);
    if (!StringUtils.hasText(requestId)) {
      requestId = UuidV7Generator.generate().toString();
    }

    response.setHeader(REQUEST_ID_HEADER, requestId);
    MDC.put(MDC_KEY, requestId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }
}
