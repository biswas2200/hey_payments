package com.heypayments.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
    UUID id,
    UUID userId,
    String tokenHash,
    UUID familyId,
    Instant expiresAt,
    Instant revokedAt,
    UUID replacedBy,
    Instant createdAt) {

  public boolean isRevoked() {
    return revokedAt != null;
  }

  public boolean isExpired(Instant now) {
    return now.isAfter(expiresAt);
  }

  public boolean isValid(Instant now) {
    return !isRevoked() && !isExpired(now);
  }
}
