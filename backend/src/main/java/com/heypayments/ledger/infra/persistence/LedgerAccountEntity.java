package com.heypayments.ledger.infra.persistence;

import com.heypayments.ledger.domain.model.AccountType;
import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ledger_accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerAccountEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "code", nullable = false, unique = true, length = 64)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 32)
  private AccountType type;

  @Column(name = "owner_id")
  private UUID ownerId;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "balance", nullable = false)
  private Long balance;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public void applyDelta(long amount) {
    long newBalance;
    try {
      newBalance = Math.addExact(this.balance != null ? this.balance : 0L, amount);
    } catch (ArithmeticException e) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "Account balance overflowed: " + this.code);
    }

    if (isNonNegativeAccountType(this.type) && newBalance < 0L) {
      throw new DomainException(
          ErrorCode.INSUFFICIENT_FUNDS,
          "Account " + this.code + " cannot have negative balance: " + newBalance);
    }

    this.balance = newBalance;
  }

  private boolean isNonNegativeAccountType(AccountType accountType) {
    return accountType == AccountType.USER_MAIN
        || accountType == AccountType.OFFLINE_ESCROW
        || accountType == AccountType.MERCHANT_PAYABLE;
  }
}
