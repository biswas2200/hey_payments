package com.heypayments.device.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.heypayments.crypto.application.ServerKeyService;
import com.heypayments.crypto.domain.CryptoService;
import com.heypayments.crypto.domain.SignedEnvelope;
import com.heypayments.crypto.domain.model.ServerKey;
import com.heypayments.crypto.domain.model.ServerKeyStatus;
import com.heypayments.device.domain.model.DeviceKind;
import com.heypayments.device.domain.model.DeviceStatus;
import com.heypayments.device.infra.persistence.DeviceEntity;
import com.heypayments.device.infra.persistence.DeviceRepository;
import com.heypayments.device.web.dto.CrlResponse;
import com.heypayments.device.web.dto.DeviceResponse;
import com.heypayments.device.web.dto.RegisterDeviceRequest;
import com.heypayments.merchant.application.MerchantService;
import com.heypayments.merchant.domain.model.Merchant;
import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeviceServiceTest {

  private DeviceRepository deviceRepository;
  private MerchantService merchantService;
  private CryptoService cryptoService;
  private ServerKeyService serverKeyService;
  private Clock clock;
  private DeviceService deviceService;

  private final Instant now = Instant.parse("2026-11-12T10:00:00Z");

  @BeforeEach
  void setUp() {
    deviceRepository = mock(DeviceRepository.class);
    merchantService = mock(MerchantService.class);
    cryptoService = mock(CryptoService.class);
    serverKeyService = mock(ServerKeyService.class);
    clock = Clock.fixed(now, ZoneId.of("UTC"));

    deviceService =
        new DeviceService(
            deviceRepository, merchantService, cryptoService, serverKeyService, clock);
  }

  @Test
  @DisplayName("registerDevice should validate coordinates and existing active devices")
  void testRegisterDeviceValidation() {
    UUID userId = UUID.randomUUID();

    // Missing coordinates
    RegisterDeviceRequest reqBadJwk =
        new RegisterDeviceRequest(Map.of("kty", "EC"), "Phone", "Agent");
    assertThatThrownBy(() -> deviceService.registerDevice(userId, "PAYER", reqBadJwk))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.VALIDATION_FAILED);

    RegisterDeviceRequest reqValid =
        new RegisterDeviceRequest(
            Map.of("kty", "EC", "x", "xCoord", "y", "yCoord"), "Phone", "Agent");
    ECPublicKey mockPub = mock(ECPublicKey.class);
    when(cryptoService.fromJwk("xCoord", "yCoord")).thenReturn(mockPub);
    when(cryptoService.toUncompressedSec1(mockPub)).thenReturn(new byte[] {4, 1, 2});

    // Active device already present
    DeviceEntity existingActive = DeviceEntity.builder().id(UUID.randomUUID()).build();
    when(deviceRepository.findByUserIdAndKindAndStatus(
            userId, DeviceKind.PAYER, DeviceStatus.ACTIVE))
        .thenReturn(Optional.of(existingActive));

    assertThatThrownBy(() -> deviceService.registerDevice(userId, "PAYER", reqValid))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.DEVICE_ALREADY_ACTIVE);
  }

  @Test
  @DisplayName("registerDevice for merchant should issue Merchant Certificate")
  void testRegisterMerchantDevice() {
    UUID userId = UUID.randomUUID();
    RegisterDeviceRequest req =
        new RegisterDeviceRequest(
            Map.of("kty", "EC", "x", "xCoord", "y", "yCoord"), "POS Terminal", "Agent");

    ECPublicKey mockPub = mock(ECPublicKey.class);
    when(cryptoService.fromJwk(anyString(), anyString())).thenReturn(mockPub);
    when(cryptoService.toUncompressedSec1(mockPub)).thenReturn(new byte[] {4, 1, 2});
    when(deviceRepository.findByUserIdAndKindAndStatus(
            userId, DeviceKind.MERCHANT, DeviceStatus.ACTIVE))
        .thenReturn(Optional.empty());

    // Merchant profile not found branch
    when(merchantService.findByUserId(userId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> deviceService.registerDevice(userId, "MERCHANT", req))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.NOT_FOUND);

    // Merchant profile present branch
    Merchant merchant =
        new Merchant(
            UUID.randomUUID(),
            userId,
            "My Shop",
            "myshop@hey",
            "GENERAL",
            com.heypayments.merchant.domain.model.MerchantStatus.ACTIVE,
            1_000_000L,
            now,
            now);
    when(merchantService.findByUserId(userId)).thenReturn(Optional.of(merchant));
    when(serverKeyService.getActiveSigningKey())
        .thenReturn(
            new ServerKey(
                "srv-1", new byte[0], new byte[0], ServerKeyStatus.ACTIVE, now, now, now));
    when(serverKeyService.signEnvelope(any()))
        .thenReturn(new SignedEnvelope(Map.of("t", "MC"), "mc-signature"));

    DeviceResponse response = deviceService.registerDevice(userId, "MERCHANT", req);
    assertThat(response.deviceId().version()).isEqualTo(7);
    assertThat(response.status()).isEqualTo("ACTIVE");
    assertThat(response.merchantCertificate()).isNotNull();
    assertThat(response.merchantCertificate().s()).isEqualTo("mc-signature");
  }

  @Test
  @DisplayName("closeDevice should validate ownership and update status")
  void testCloseDevice() {
    UUID userId = UUID.randomUUID();
    UUID deviceId = UUID.randomUUID();

    when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> deviceService.closeDevice(userId, deviceId))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.NOT_FOUND);

    DeviceEntity device =
        DeviceEntity.builder()
            .id(deviceId)
            .userId(UUID.randomUUID()) // Different user
            .status(DeviceStatus.ACTIVE)
            .build();
    when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

    assertThatThrownBy(() -> deviceService.closeDevice(userId, deviceId))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.FORBIDDEN);

    device.setUserId(userId);
    deviceService.closeDevice(userId, deviceId);
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.CLOSED);
    verify(deviceRepository).save(device);
  }

  @Test
  @DisplayName("getActiveDevice and getCrl branches")
  void testGetActiveDeviceAndCrl() {
    UUID userId = UUID.randomUUID();
    when(deviceRepository.findAllByUserId(userId)).thenReturn(List.of());

    assertThatThrownBy(() -> deviceService.getActiveDevice(userId))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.NOT_FOUND);

    DeviceEntity active =
        DeviceEntity.builder()
            .id(UUID.randomUUID())
            .status(DeviceStatus.ACTIVE)
            .lastCounter(5L)
            .build();
    when(deviceRepository.findAllByUserId(userId)).thenReturn(List.of(active));

    DeviceResponse resp = deviceService.getActiveDevice(userId);
    assertThat(resp.deviceId()).isEqualTo(active.getId());
    assertThat(resp.lastCounter()).isEqualTo(5L);

    // Active merchant device should return populated merchant certificate
    UUID merchantUserId = UUID.randomUUID();
    DeviceEntity activeMerchantDevice =
        DeviceEntity.builder()
            .id(UUID.randomUUID())
            .userId(merchantUserId)
            .kind(DeviceKind.MERCHANT)
            .publicKey(new byte[65])
            .status(DeviceStatus.ACTIVE)
            .lastCounter(0L)
            .build();
    when(deviceRepository.findAllByUserId(merchantUserId)).thenReturn(List.of(activeMerchantDevice));
    when(merchantService.findByUserId(merchantUserId))
        .thenReturn(
            Optional.of(
                new Merchant(
                    UUID.randomUUID(),
                    merchantUserId,
                    "My Shop",
                    "myshop@hey",
                    "GENERAL",
                    com.heypayments.merchant.domain.model.MerchantStatus.ACTIVE,
                    100000L,
                    now,
                    now)));
    when(serverKeyService.getActiveSigningKey())
        .thenReturn(
            new ServerKey(
                "srv-1", new byte[0], new byte[0], ServerKeyStatus.ACTIVE, now, now, now));
    when(serverKeyService.signEnvelope(any()))
        .thenReturn(new SignedEnvelope(Map.of("t", "MC"), "mc-signature"));

    DeviceResponse merchantResp = deviceService.getActiveDevice(merchantUserId);
    assertThat(merchantResp.merchantCertificate()).isNotNull();
    assertThat(merchantResp.merchantCertificate().s()).isEqualTo("mc-signature");

    // CRL with and without since
    DeviceEntity revoked =
        DeviceEntity.builder()
            .id(UUID.randomUUID())
            .status(DeviceStatus.REVOKED)
            .updatedAt(now)
            .build();
    when(deviceRepository.findAllRevokedDevices()).thenReturn(List.of(revoked));
    when(deviceRepository.findRevokedDevicesSince(now)).thenReturn(List.of(revoked));

    CrlResponse crlAll = deviceService.getCrl(null);
    assertThat(crlAll.revoked()).hasSize(1);

    CrlResponse crlSince = deviceService.getCrl(now);
    assertThat(crlSince.revoked()).hasSize(1);
  }
}
