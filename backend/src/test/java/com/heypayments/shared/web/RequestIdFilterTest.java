package com.heypayments.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

  private final RequestIdFilter filter = new RequestIdFilter();

  @BeforeEach
  void setUp() {
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  @DisplayName("should propagate existing valid X-Request-Id header to response and MDC")
  void shouldPropagateExistingRequestId() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "req-12345");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(
        request,
        response,
        (req, res) -> {
          assertThat(MDC.get("requestId")).isEqualTo("req-12345");
        });

    assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("req-12345");
    assertThat(MDC.get("requestId")).isNull(); // Cleaned up in finally
  }

  @Test
  @DisplayName("should generate UUIDv7 when X-Request-Id header is missing")
  void shouldGenerateRequestIdWhenMissing() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(
        request,
        response,
        (req, res) -> {
          assertThat(MDC.get("requestId")).isNotBlank();
        });

    String generatedId = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
    assertThat(generatedId).isNotBlank();
    assertThat(MDC.get("requestId")).isNull();
  }

  @Test
  @DisplayName("should reject and replace malicious or invalid X-Request-Id header")
  void shouldSanitizeMaliciousRequestId() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "bad\r\nInjected-Header: value");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(
        request,
        response,
        (req, res) -> {
          String mdcId = MDC.get("requestId");
          assertThat(mdcId).isNotBlank();
          assertThat(mdcId).doesNotContain("\r", "\n", "\"", ":");
        });

    String responseId = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
    assertThat(responseId).isNotBlank();
    assertThat(responseId).doesNotContain("\r", "\n", "\"", ":");
  }

  @Test
  @DisplayName("should reject oversized X-Request-Id header")
  void shouldRejectOversizedRequestId() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "a".repeat(128));
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(
        request,
        response,
        (req, res) -> {
          String mdcId = MDC.get("requestId");
          assertThat(mdcId).hasSizeLessThanOrEqualTo(64);
          assertThat(mdcId).isNotEqualTo("a".repeat(128));
        });

    String responseId = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
    assertThat(responseId).hasSizeLessThanOrEqualTo(64);
  }
}
