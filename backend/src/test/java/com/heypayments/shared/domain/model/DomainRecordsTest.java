package com.heypayments.shared.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.heypayments.auth.domain.model.RefreshToken;
import com.heypayments.auth.domain.model.User;
import com.heypayments.auth.domain.model.UserRole;
import com.heypayments.auth.domain.model.UserStatus;
import com.heypayments.crypto.domain.SignedEnvelope;
import com.heypayments.crypto.domain.model.ServerKey;
import com.heypayments.crypto.domain.model.ServerKeyStatus;
import com.heypayments.device.domain.model.Device;
import com.heypayments.device.domain.model.DeviceKind;
import com.heypayments.device.domain.model.DeviceStatus;
import com.heypayments.merchant.domain.model.Merchant;
import com.heypayments.merchant.domain.model.MerchantStatus;
import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DomainRecordsTest {

  @Test
  @DisplayName("should test ServerKey record equality, hashCode, and toString")
  void testServerKeyRecord() {
    Instant now = Instant.parse("2026-11-12T10:00:00Z");
    byte[] pubKey = new byte[] {1, 2};
    byte[] privKey = new byte[] {3, 4};

    ServerKey key1 =
        new ServerKey(
            "srv-1", pubKey, privKey, ServerKeyStatus.ACTIVE, now, now.plusSeconds(3600), now);
    ServerKey key2 =
        new ServerKey(
            "srv-1", pubKey, privKey, ServerKeyStatus.ACTIVE, now, now.plusSeconds(3600), now);
    ServerKey key3 =
        new ServerKey(
            "srv-2", pubKey, privKey, ServerKeyStatus.ACTIVE, now, now.plusSeconds(3600), now);

    assertThat(key1).isEqualTo(key1);
    assertThat(key1).isEqualTo(key2);
    assertThat(key1).hasSameHashCodeAs(key2);
    assertThat(key1).isNotEqualTo(key3);
    assertThat(key1).isNotEqualTo(null);
    assertThat(key1).isNotEqualTo("other-type");
    assertThat(key1.toString()).contains("srv-1");
  }

  @Test
  @DisplayName("should test RefreshToken record equality, hashCode, and toString")
  void testRefreshTokenRecord() {
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID familyId = UUID.randomUUID();
    Instant now = Instant.now();

    RefreshToken token1 =
        new RefreshToken(id, userId, "h1", familyId, now.plusSeconds(3600), null, null, now);
    RefreshToken token2 =
        new RefreshToken(id, userId, "h1", familyId, now.plusSeconds(3600), null, null, now);
    RefreshToken token3 =
        new RefreshToken(id, userId, "h2", familyId, now.plusSeconds(3600), now, null, now);

    assertThat(token1).isEqualTo(token2);
    assertThat(token1).hasSameHashCodeAs(token2);
    assertThat(token1).isNotEqualTo(token3);
    assertThat(token1.toString()).contains(userId.toString());
    assertThat(token1.isRevoked()).isFalse();
    assertThat(token3.isRevoked()).isTrue();
    assertThat(token1.isExpired(now.plusSeconds(7200))).isTrue();
    assertThat(token1.isValid(now)).isTrue();
  }

  @Test
  @DisplayName("should test User, Device, Merchant, and SignedEnvelope records")
  void testOtherDomainRecords() {
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();

    User u1 =
        new User(
            id, "+919876543210", null, "hash", UserRole.PAYER, UserStatus.ACTIVE, null, now, now);
    User u2 =
        new User(
            id, "+919876543210", null, "hash", UserRole.PAYER, UserStatus.ACTIVE, null, now, now);
    assertThat(u1).isEqualTo(u2);
    assertThat(u1).hasSameHashCodeAs(u2);

    byte[] devPub = new byte[] {4};
    Device d1 =
        new Device(
            id,
            id,
            DeviceKind.PAYER,
            devPub,
            "Pixel",
            "Agent",
            DeviceStatus.ACTIVE,
            0L,
            null,
            null,
            now,
            now);
    Device d2 =
        new Device(
            id,
            id,
            DeviceKind.PAYER,
            devPub,
            "Pixel",
            "Agent",
            DeviceStatus.ACTIVE,
            0L,
            null,
            null,
            now,
            now);
    assertThat(d1).isEqualTo(d2);
    assertThat(d1.isActive()).isTrue();

    Merchant m1 =
        new Merchant(
            id, id, "Store", "store@hey", "GENERAL", MerchantStatus.ACTIVE, 1000L, now, now);
    Merchant m2 =
        new Merchant(
            id, id, "Store", "store@hey", "GENERAL", MerchantStatus.ACTIVE, 1000L, now, now);
    assertThat(m1).isEqualTo(m2);
    assertThat(m1.isActive()).isTrue();

    SignedEnvelope e1 = new SignedEnvelope(Map.of("a", 1), "sig");
    SignedEnvelope e2 = new SignedEnvelope(Map.of("a", 1), "sig");
    assertThat(e1).isEqualTo(e2);
    assertThat(e1.toString()).contains("sig");

    DomainException ex = new DomainException(ErrorCode.VALIDATION_FAILED, "test error");
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
  }
}
