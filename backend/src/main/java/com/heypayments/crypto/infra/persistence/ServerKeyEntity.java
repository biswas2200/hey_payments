package com.heypayments.crypto.infra.persistence;

import com.heypayments.crypto.domain.model.ServerKeyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "server_keys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerKeyEntity {

  @Id
  @Column(name = "kid", nullable = false, length = 50)
  private String kid;

  @Column(name = "public_key", nullable = false)
  private byte[] publicKey;

  @Column(name = "private_key_enc", nullable = false)
  private byte[] privateKeyEnc;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ServerKeyStatus status;

  @Column(name = "not_before", nullable = false)
  private Instant notBefore;

  @Column(name = "not_after", nullable = false)
  private Instant notAfter;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
