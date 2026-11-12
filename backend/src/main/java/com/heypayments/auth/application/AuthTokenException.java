package com.heypayments.auth.application;

import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;

public class AuthTokenException extends DomainException {

  public AuthTokenException(String message) {
    super(ErrorCode.UNAUTHENTICATED, message);
  }

  public AuthTokenException(String message, Throwable cause) {
    super(ErrorCode.UNAUTHENTICATED, message);
    initCause(cause);
  }
}
