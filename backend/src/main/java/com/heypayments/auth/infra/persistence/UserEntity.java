package com.heypayments.auth.infra.persistence;

import com.heypayments.auth.domain.model.UserRole;
import com.heypayments.auth.domain.model.UserStatus;
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
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "phone", nullable = false, unique = true, length = 20)
  private String phone;

  @Column(name = "email", length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private UserRole role;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private UserStatus status;

  @Column(name = "pin_hash", length = 255)
  private String pinHash;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
