package com.heypayments.auth.application;

public class AuthTokenException extends RuntimeException {

  public AuthTokenException(String message) {
    super(message);
  }

  public AuthTokenException(String message, Throwable cause) {
    super(message, cause);
  }
}
