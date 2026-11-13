package com.heypayments.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record User(
    UUID id,
    String phone,
    String email,
    String passwordHash,
    UserRole role,
    UserStatus status,
    String pinHash,
    Instant createdAt,
    Instant updatedAt) {

  public boolean isActive() {
    return status == UserStatus.ACTIVE;
  }
}
