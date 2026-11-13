package com.heypayments.ledger.domain.model;

import java.time.Instant;
import java.util.UUID;

public record JournalLine(
    UUID id, UUID accountId, String accountCode, long amount, Instant createdAt) {}
