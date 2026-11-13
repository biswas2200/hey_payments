package com.heypayments.ledger.infra.persistence;

import com.heypayments.ledger.domain.model.EntryKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "journal_entries")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false, length = 32)
  private EntryKind kind;

  @Column(name = "reference", nullable = false, unique = true, length = 128)
  private String reference;

  @Column(name = "posted_at", nullable = false, updatable = false)
  private Instant postedAt;

  @Column(name = "memo", length = 255)
  private String memo;
}
