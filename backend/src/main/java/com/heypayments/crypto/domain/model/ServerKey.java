package com.heypayments.crypto.domain.model;

import java.time.Instant;

public record ServerKey(
    String kid,
    byte[] publicKey,
    byte[] privateKeyEnc,
    ServerKeyStatus status,
    Instant notBefore,
    Instant notAfter,
    Instant createdAt) {

  public boolean isActive(Instant now) {
    return status == ServerKeyStatus.ACTIVE && !now.isBefore(notBefore) && !now.isAfter(notAfter);
  }

  public boolean canVerify(Instant now) {
    return (status == ServerKeyStatus.ACTIVE || status == ServerKeyStatus.RETIRING)
        && !now.isBefore(notBefore);
  }
}
