package com.heypayments.shared.domain.exception;

/**
 * Standard business error codes mapped to HTTP status codes and RFC 9457 error types. Source of
 * truth: docs/06-api-spec.md §Error codes.
 */
public enum ErrorCode {
  VALIDATION_FAILED(400, "Validation Failed", "validation-failed"),
  UNAUTHENTICATED(401, "Unauthenticated", "unauthenticated"),
  TOKEN_EXPIRED(401, "Token Expired", "token-expired"),
  FORBIDDEN(403, "Forbidden", "forbidden"),
  DEVICE_FROZEN(403, "Device Frozen", "device-frozen"),
  NOT_FOUND(404, "Not Found", "not-found"),
  SYNC_REQUIRED(409, "Sync Required", "sync-required"),
  DEVICE_ALREADY_ACTIVE(409, "Device Already Active", "device-already-active"),
  CONFLICT(409, "Conflict", "conflict"),
  LIMIT_EXCEEDED(422, "Limit Exceeded", "limit-exceeded"),
  INSUFFICIENT_FUNDS(422, "Insufficient Funds", "insufficient-funds"),
  IDEMPOTENCY_KEY_REUSED(422, "Idempotency Key Reused", "idempotency-key-reused"),
  RATE_LIMITED(429, "Rate Limited", "rate-limited"),
  METHOD_NOT_ALLOWED(405, "Method Not Allowed", "method-not-allowed"),
  INTERNAL(500, "Internal Server Error", "internal");

  private final int statusCode;
  private final String title;
  private final String slug;

  ErrorCode(int statusCode, String title, String slug) {
    this.statusCode = statusCode;
    this.title = title;
    this.slug = slug;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getTitle() {
    return title;
  }

  public String getTypeUri() {
    return "https://heypayments.dev/errors/" + slug;
  }
}
