package com.heypayments.shared.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Standard business error codes mapped to HTTP status codes and RFC 9457 error types. Source of
 * truth: docs/06-api-spec.md §Error codes.
 */
public enum ErrorCode {
  VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation Failed", "validation-failed"),
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Unauthenticated", "unauthenticated"),
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token Expired", "token-expired"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden", "forbidden"),
  DEVICE_FROZEN(HttpStatus.FORBIDDEN, "Device Frozen", "device-frozen"),
  NOT_FOUND(HttpStatus.NOT_FOUND, "Not Found", "not-found"),
  SYNC_REQUIRED(HttpStatus.CONFLICT, "Sync Required", "sync-required"),
  DEVICE_ALREADY_ACTIVE(HttpStatus.CONFLICT, "Device Already Active", "device-already-active"),
  CONFLICT(HttpStatus.CONFLICT, "Conflict", "conflict"),
  LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "Limit Exceeded", "limit-exceeded"),
  INSUFFICIENT_FUNDS(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient Funds", "insufficient-funds"),
  IDEMPOTENCY_KEY_REUSED(
      HttpStatus.UNPROCESSABLE_ENTITY, "Idempotency Key Reused", "idempotency-key-reused"),
  RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Rate Limited", "rate-limited"),
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", "method-not-allowed"),
  INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "internal");

  private final HttpStatus httpStatus;
  private final String title;
  private final String slug;

  ErrorCode(HttpStatus httpStatus, String title, String slug) {
    this.httpStatus = httpStatus;
    this.title = title;
    this.slug = slug;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public int getStatusCode() {
    return httpStatus.value();
  }

  public String getTitle() {
    return title;
  }

  public String getTypeUri() {
    return "https://heypayments.dev/errors/" + slug;
  }
}
