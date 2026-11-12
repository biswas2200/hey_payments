package com.heypayments.shared.domain.exception;

import java.util.Collections;
import java.util.List;

/**
 * Base domain exception representing business and protocol errors. Mapped to RFC 9457 Problem
 * Details by GlobalExceptionHandler.
 */
public class DomainException extends RuntimeException {

  private final ErrorCode errorCode;
  private final List<FieldErrorDetail> fieldErrors;

  /**
   * Constructs a domain exception with error code and detail message.
   *
   * @param errorCode the business error code
   * @param detail the error detail description
   */
  public DomainException(ErrorCode errorCode, String detail) {
    super(detail);
    this.errorCode = errorCode;
    this.fieldErrors = Collections.emptyList();
  }

  /**
   * Constructs a domain exception with error code, detail message, and cause.
   *
   * @param errorCode the business error code
   * @param detail the error detail description
   * @param cause root cause exception
   */
  public DomainException(ErrorCode errorCode, String detail, Throwable cause) {
    super(detail, cause);
    this.errorCode = errorCode;
    this.fieldErrors = Collections.emptyList();
  }

  /**
   * Constructs a domain exception with field-level validation errors.
   *
   * @param errorCode the business error code
   * @param detail the error detail description
   * @param fieldErrors list of field-level errors
   */
  public DomainException(ErrorCode errorCode, String detail, List<FieldErrorDetail> fieldErrors) {
    super(detail);
    this.errorCode = errorCode;
    this.fieldErrors = fieldErrors != null ? List.copyOf(fieldErrors) : Collections.emptyList();
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public List<FieldErrorDetail> getFieldErrors() {
    return fieldErrors;
  }
}
