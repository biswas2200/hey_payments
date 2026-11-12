package com.heypayments.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler exceptionHandler;
  private MockHttpServletRequest request;

  @BeforeEach
  void setUp() {
    MDC.clear();
    exceptionHandler = new GlobalExceptionHandler();
    request = new MockHttpServletRequest();
    request.setRequestURI("/api/v1/offline/load");
    MDC.put(RequestIdFilter.MDC_KEY, "test-req-id-123");
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  @DisplayName("should map DomainException to RFC 9457 Problem Details")
  void shouldMapDomainException() {
    DomainException ex =
        new DomainException(ErrorCode.LIMIT_EXCEEDED, "Offline balance cannot exceed ₹2,000");

    ResponseEntity<ProblemResponse> response = exceptionHandler.handleDomainException(ex, request);

    assertThat(response.getStatusCode().value()).isEqualTo(422);
    ProblemResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo(422);
    assertThat(body.code()).isEqualTo("LIMIT_EXCEEDED");
    assertThat(body.title()).isEqualTo("Limit Exceeded");
    assertThat(body.detail()).isEqualTo("Offline balance cannot exceed ₹2,000");
    assertThat(body.instance()).isEqualTo("/api/v1/offline/load");
    assertThat(body.type()).isEqualTo("https://heypayments.dev/errors/limit-exceeded");
    assertThat(body.requestId()).isEqualTo("test-req-id-123");
  }

  @Test
  @DisplayName(
      "should map HttpMessageNotReadableException to 400 VALIDATION_FAILED, not 500 INTERNAL")
  void shouldMapHttpMessageNotReadableException() {
    HttpMessageNotReadableException ex =
        new HttpMessageNotReadableException(
            "JSON parse error: Unexpected character", new MockHttpInputMessage(new byte[0]));

    ResponseEntity<ProblemResponse> response =
        exceptionHandler.handleHttpMessageNotReadable(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ProblemResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo(400);
    assertThat(body.code()).isEqualTo("VALIDATION_FAILED");
    assertThat(body.requestId()).isEqualTo("test-req-id-123");
  }

  @Test
  @DisplayName("should map NoResourceFoundException to 404 NOT_FOUND")
  void shouldMapNoResourceFoundException() {
    NoResourceFoundException ex =
        new NoResourceFoundException(HttpMethod.GET, "/api/v1/unknown", "Resource not found");

    ResponseEntity<ProblemResponse> response =
        exceptionHandler.handleNoResourceFoundException(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    ProblemResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo(404);
    assertThat(body.code()).isEqualTo("NOT_FOUND");
    assertThat(body.requestId()).isEqualTo("test-req-id-123");
  }

  @Test
  @DisplayName("should map HttpRequestMethodNotSupportedException to 405 METHOD_NOT_ALLOWED")
  void shouldMapHttpRequestMethodNotSupportedException() {
    HttpRequestMethodNotSupportedException ex =
        new HttpRequestMethodNotSupportedException("DELETE");

    ResponseEntity<ProblemResponse> response =
        exceptionHandler.handleMethodNotSupported(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    ProblemResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo(405);
    assertThat(body.code()).isEqualTo("METHOD_NOT_ALLOWED");
  }

  @Test
  @DisplayName("should map AccessDeniedException to 403 FORBIDDEN")
  void shouldMapAccessDeniedException() {
    AccessDeniedException ex = new AccessDeniedException("Access is denied");

    ResponseEntity<ProblemResponse> response = exceptionHandler.handleAccessDenied(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    ProblemResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo(403);
    assertThat(body.code()).isEqualTo("FORBIDDEN");
  }

  @Test
  @DisplayName("should map unhandled Exception to 500 without leaking stack traces")
  void shouldMapUnhandledException() {
    RuntimeException ex = new RuntimeException("Sensitive DB connection failed: password=secret");

    ResponseEntity<ProblemResponse> response = exceptionHandler.handleGenericException(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    ProblemResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo(500);
    assertThat(body.code()).isEqualTo("INTERNAL");
    assertThat(body.title()).isEqualTo("Internal Server Error");
    assertThat(body.detail()).isEqualTo("An unexpected internal error occurred.");
    assertThat(body.detail()).doesNotContain("password");
    assertThat(body.requestId()).isEqualTo("test-req-id-123");
  }
}
