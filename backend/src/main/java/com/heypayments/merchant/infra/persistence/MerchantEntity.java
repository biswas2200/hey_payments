package com.heypayments.merchant.infra.persistence;

import com.heypayments.merchant.domain.model.MerchantStatus;
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
import lombok.Setter;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false, unique = true, updatable = false)
  private UUID userId;

  @Column(name = "display_name", nullable = false, length = 60)
  private String displayName;

  @Column(name = "handle", nullable = false, unique = true, length = 60)
  private String handle;

  @Column(name = "category", nullable = false, length = 50)
  private String category;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private MerchantStatus status;

  @Column(name = "pending_cap", nullable = false)
  private long pendingCap;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
