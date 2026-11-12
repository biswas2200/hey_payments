package com.heypayments.merchant.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Merchant(
    UUID id,
    UUID userId,
    String displayName,
    String handle,
    String category,
    MerchantStatus status,
    long pendingCap,
    Instant createdAt,
    Instant updatedAt) {

  public boolean isActive() {
    return status == MerchantStatus.ACTIVE;
  }
}
