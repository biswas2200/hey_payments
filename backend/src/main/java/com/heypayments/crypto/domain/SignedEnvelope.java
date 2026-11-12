package com.heypayments.crypto.domain;

import java.util.Map;

public record SignedEnvelope(Map<String, Object> p, String s) {

  public SignedEnvelope {
    if (p == null) {
      throw new IllegalArgumentException("Payload 'p' cannot be null");
    }
    if (s == null || s.isBlank()) {
      throw new IllegalArgumentException("Signature 's' cannot be null or blank");
    }
  }
}
