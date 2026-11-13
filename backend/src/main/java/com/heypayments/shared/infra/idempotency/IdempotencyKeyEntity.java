package com.heypayments.shared.infra.idempotency;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "idempotency_keys")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKeyEntity {

  @Id
  @Column(name = "key", nullable = false, length = 128)
  private String key;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "request_hash", length = 64)
  private String requestHash;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "response", columnDefinition = "jsonb")
  private String response;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
