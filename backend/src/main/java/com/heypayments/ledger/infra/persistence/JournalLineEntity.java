package com.heypayments.ledger.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "journal_lines")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalLineEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "entry_id", nullable = false, updatable = false)
  private UUID entryId;

  @Column(name = "account_id", nullable = false, updatable = false)
  private UUID accountId;

  @Column(name = "amount", nullable = false, updatable = false)
  private Long amount;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
