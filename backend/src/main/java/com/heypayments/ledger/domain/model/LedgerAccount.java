package com.heypayments.ledger.domain.model;

import com.heypayments.shared.domain.model.Money;
import java.time.Instant;
import java.util.UUID;

public record LedgerAccount(
    UUID id,
    String code,
    AccountType type,
    UUID ownerId,
    String currency,
    long balancePaise,
    long version,
    Instant createdAt,
    Instant updatedAt) {

  public Money moneyBalance() {
    return Money.ofPaise(balancePaise);
  }
}
