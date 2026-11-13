package com.heypayments.merchant.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.heypayments.BaseIntegrationTest;
import com.heypayments.merchant.domain.model.MerchantStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MerchantRepositoryTest extends BaseIntegrationTest {

  @Autowired private MerchantRepository merchantRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("should persist and find merchant by user id and handle")
  void shouldManageMerchants() {
    UUID userId = UUID.randomUUID();
    Timestamp now = Timestamp.from(Instant.now());
    jdbcTemplate.update(
        "INSERT INTO users (id, phone, password_hash, role, status, created_at, updated_at) VALUES"
            + " (?, ?, ?, ?, ?, ?, ?)",
        userId,
        "+919876543240",
        "hash",
        "MERCHANT",
        "ACTIVE",
        now,
        now);

    UUID merchantId = UUID.randomUUID();
    MerchantEntity merchant =
        MerchantEntity.builder()
            .id(merchantId)
            .userId(userId)
            .displayName("Sharma Sweets")
            .handle("sharma.sweets@hey")
            .category("FOOD")
            .status(MerchantStatus.ACTIVE)
            .pendingCap(1000000L)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    merchantRepository.save(merchant);

    Optional<MerchantEntity> byUserId = merchantRepository.findByUserId(userId);
    assertThat(byUserId).isPresent();
    assertThat(byUserId.get().getHandle()).isEqualTo("sharma.sweets@hey");

    Optional<MerchantEntity> byHandle = merchantRepository.findByHandle("sharma.sweets@hey");
    assertThat(byHandle).isPresent();
    assertThat(byHandle.get().getId()).isEqualTo(merchantId);
  }
}
