package com.heypayments.wallet.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Wallet(UUID id, UUID userId, String status, Instant createdAt, Instant updatedAt) {}
