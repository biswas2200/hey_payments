package com.heypayments.device.application;

import com.heypayments.crypto.application.ServerKeyService;
import com.heypayments.crypto.domain.CryptoException;
import com.heypayments.crypto.domain.CryptoService;
import com.heypayments.crypto.domain.SignedEnvelope;
import com.heypayments.device.domain.model.DeviceKind;
import com.heypayments.device.domain.model.DeviceStatus;
import com.heypayments.device.infra.persistence.DeviceEntity;
import com.heypayments.device.infra.persistence.DeviceRepository;
import com.heypayments.device.web.dto.CrlEntryDto;
import com.heypayments.device.web.dto.CrlResponse;
import com.heypayments.device.web.dto.DeviceResponse;
import com.heypayments.device.web.dto.RegisterDeviceRequest;
import com.heypayments.merchant.application.MerchantService;
import com.heypayments.merchant.domain.model.Merchant;
import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import com.heypayments.shared.infra.id.UuidV7Generator;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

  private final DeviceRepository deviceRepository;
  private final MerchantService merchantService;
  private final CryptoService cryptoService;
  private final ServerKeyService serverKeyService;
  private final Clock clock;

  @Transactional
  public DeviceResponse registerDevice(UUID userId, String roleName, RegisterDeviceRequest req) {
    Map<String, String> jwk = req.publicKeyJwk();
    if (!jwk.containsKey("x") || !jwk.containsKey("y")) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "publicKeyJwk must contain x and y coordinates");
    }

    ECPublicKey pubKey;
    byte[] sec1Bytes;
    try {
      pubKey = cryptoService.fromJwk(jwk.get("x"), jwk.get("y"));
      sec1Bytes = cryptoService.toUncompressedSec1(pubKey);
    } catch (CryptoException | IllegalArgumentException e) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "Invalid EC public key coordinates: " + e.getMessage());
    }

    DeviceKind kind = DeviceKind.valueOf(roleName);
    Optional<DeviceEntity> activeDevice =
        deviceRepository.findByUserIdAndKindAndStatus(userId, kind, DeviceStatus.ACTIVE);

    if (activeDevice.isPresent()) {
      throw new DomainException(
          ErrorCode.DEVICE_ALREADY_ACTIVE,
          "An active device already exists for this account. Revoke or close the existing device first.");
    }

    Instant now = clock.instant();
    UUID deviceId = UuidV7Generator.generate();

    DeviceEntity device =
        DeviceEntity.builder()
            .id(deviceId)
            .userId(userId)
            .kind(kind)
            .publicKey(sec1Bytes)
            .label(req.label())
            .userAgent(req.userAgent())
            .status(DeviceStatus.ACTIVE)
            .lastCounter(0L)
            .createdAt(now)
            .updatedAt(now)
            .build();
    deviceRepository.save(device);

    SignedEnvelope merchantCert = null;
    if (kind == DeviceKind.MERCHANT) {
      Merchant merchant =
          merchantService
              .findByUserId(userId)
              .orElseThrow(
                  () -> new DomainException(ErrorCode.NOT_FOUND, "Merchant profile not found"));

      Map<String, Object> mcPayload = new LinkedHashMap<>();
      mcPayload.put("v", 1);
      mcPayload.put("t", "MC");
      mcPayload.put("merchantId", merchant.id().toString());
      mcPayload.put("deviceId", deviceId.toString());
      mcPayload.put("devicePub", Base64.getUrlEncoder().withoutPadding().encodeToString(sec1Bytes));
      mcPayload.put("displayName", merchant.displayName());
      mcPayload.put("vpaLike", merchant.handle());
      mcPayload.put("issuedAt", now.toString());
      mcPayload.put("expiresAt", now.plus(30, ChronoUnit.DAYS).toString());
      mcPayload.put("kid", serverKeyService.getActiveSigningKey().kid());

      merchantCert = serverKeyService.signEnvelope(mcPayload);
      log.info(
          "Issued Merchant Certificate for device id={} merchant id={}", deviceId, merchant.id());
    }

    log.info("Registered device id={} user id={} kind={}", deviceId, userId, kind);
    return new DeviceResponse(deviceId, "ACTIVE", 0L, merchantCert);
  }

  @Transactional(readOnly = true)
  public DeviceResponse getActiveDevice(UUID userId) {
    List<DeviceEntity> devices = deviceRepository.findAllByUserId(userId);
    DeviceEntity active =
        devices.stream()
            .filter(d -> d.getStatus() == DeviceStatus.ACTIVE)
            .findFirst()
            .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "No active device found"));

    SignedEnvelope merchantCert = null;
    if (active.getKind() == DeviceKind.MERCHANT) {
      Merchant merchant =
          merchantService
              .findByUserId(userId)
              .orElseThrow(
                  () -> new DomainException(ErrorCode.NOT_FOUND, "Merchant profile not found"));

      Instant now = clock.instant();
      Map<String, Object> mcPayload = new LinkedHashMap<>();
      mcPayload.put("v", 1);
      mcPayload.put("t", "MC");
      mcPayload.put("merchantId", merchant.id().toString());
      mcPayload.put("deviceId", active.getId().toString());
      mcPayload.put(
          "devicePub",
          Base64.getUrlEncoder().withoutPadding().encodeToString(active.getPublicKey()));
      mcPayload.put("displayName", merchant.displayName());
      mcPayload.put("vpaLike", merchant.handle());
      mcPayload.put("issuedAt", now.toString());
      mcPayload.put("expiresAt", now.plus(30, ChronoUnit.DAYS).toString());
      mcPayload.put("kid", serverKeyService.getActiveSigningKey().kid());

      merchantCert = serverKeyService.signEnvelope(mcPayload);
    }

    return new DeviceResponse(
        active.getId(), active.getStatus().name(), active.getLastCounter(), merchantCert);
  }

  @Transactional
  public void closeDevice(UUID userId, UUID deviceId) {
    DeviceEntity device =
        deviceRepository
            .findById(deviceId)
            .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Device not found"));

    if (!device.getUserId().equals(userId)) {
      throw new DomainException(ErrorCode.FORBIDDEN, "Not authorized to close this device");
    }

    device.setStatus(DeviceStatus.CLOSED);
    device.setUpdatedAt(clock.instant());
    deviceRepository.save(device);
    log.info("Device id={} closed by user id={}", deviceId, userId);
  }

  @Transactional(readOnly = true)
  public CrlResponse getCrl(Instant since) {
    List<DeviceEntity> revokedDevices =
        (since != null)
            ? deviceRepository.findRevokedDevicesSince(since)
            : deviceRepository.findAllRevokedDevices();

    List<CrlEntryDto> entries =
        revokedDevices.stream()
            .map(d -> new CrlEntryDto(d.getId(), d.getStatus().name(), d.getUpdatedAt()))
            .toList();

    return new CrlResponse(clock.instant(), entries);
  }
}
