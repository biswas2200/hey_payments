package com.heypayments.device.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Device(
    UUID id,
    UUID userId,
    DeviceKind kind,
    byte[] publicKey,
    String label,
    String userAgent,
    DeviceStatus status,
    long lastCounter,
    Instant frozenAt,
    String frozenReason,
    Instant createdAt,
    Instant updatedAt) {

  public boolean isActive() {
    return status == DeviceStatus.ACTIVE;
  }
}
