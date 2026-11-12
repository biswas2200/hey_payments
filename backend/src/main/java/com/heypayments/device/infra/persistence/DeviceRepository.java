package com.heypayments.device.infra.persistence;

import com.heypayments.device.domain.model.DeviceKind;
import com.heypayments.device.domain.model.DeviceStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID> {

  Optional<DeviceEntity> findByUserIdAndKindAndStatus(
      UUID userId, DeviceKind kind, DeviceStatus status);

  List<DeviceEntity> findAllByUserId(UUID userId);

  @Query(
      "SELECT d FROM DeviceEntity d WHERE d.status IN ('FROZEN', 'REVOKED', 'CLOSED') AND d.updatedAt >= :since")
  List<DeviceEntity> findRevokedDevicesSince(@Param("since") Instant since);

  @Query("SELECT d FROM DeviceEntity d WHERE d.status IN ('FROZEN', 'REVOKED', 'CLOSED')")
  List<DeviceEntity> findAllRevokedDevices();
}
