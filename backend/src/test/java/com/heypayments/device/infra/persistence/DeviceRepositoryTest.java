package com.heypayments.device.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.heypayments.BaseIntegrationTest;
import com.heypayments.device.domain.model.DeviceKind;
import com.heypayments.device.domain.model.DeviceStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class DeviceRepositoryTest extends BaseIntegrationTest {

  @Autowired private DeviceRepository deviceRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("should persist and find active device for user and kind")
  void shouldManageDevices() {
    UUID userId = UUID.randomUUID();
    Timestamp now = Timestamp.from(Instant.now());
    jdbcTemplate.update(
        "INSERT INTO users (id, phone, password_hash, role, status, created_at, updated_at) VALUES"
            + " (?, ?, ?, ?, ?, ?, ?)",
        userId,
        "+919876543230",
        "hash",
        "PAYER",
        "ACTIVE",
        now,
        now);

    UUID devId = UUID.randomUUID();
    DeviceEntity device =
        DeviceEntity.builder()
            .id(devId)
            .userId(userId)
            .kind(DeviceKind.PAYER)
            .publicKey(new byte[] {0x04, 0x11, 0x22})
            .label("Pixel 8")
            .userAgent("Mozilla/5.0")
            .status(DeviceStatus.ACTIVE)
            .lastCounter(0L)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    deviceRepository.save(device);

    Optional<DeviceEntity> activeDevice =
        deviceRepository.findByUserIdAndKindAndStatus(userId, DeviceKind.PAYER, DeviceStatus.ACTIVE);
    assertThat(activeDevice).isPresent();
    assertThat(activeDevice.get().getId()).isEqualTo(devId);
    assertThat(activeDevice.get().getLabel()).isEqualTo("Pixel 8");
  }
}
