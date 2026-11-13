package com.heypayments.crypto.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.heypayments.BaseIntegrationTest;
import com.heypayments.crypto.domain.model.ServerKey;
import com.heypayments.crypto.domain.model.ServerKeyStatus;
import com.heypayments.crypto.infra.persistence.ServerKeyRepository;
import java.security.interfaces.ECPublicKey;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ServerKeyServiceTest extends BaseIntegrationTest {

  @Autowired private ServerKeyService serverKeyService;
  @Autowired private ServerKeyRepository serverKeyRepository;
  @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

  @BeforeEach
  @AfterEach
  void cleanUp() {
    serverKeyRepository.deleteAll();
  }

  @Test
  @DisplayName("should ensure active key exists, encrypt it at rest, and expose JWKS")
  void shouldManageActiveKeyAndJwks() {
    ServerKey activeKey = serverKeyService.getActiveSigningKey();
    assertThat(activeKey).isNotNull();
    assertThat(activeKey.kid()).isNotBlank();
    assertThat(activeKey.status()).isEqualTo(ServerKeyStatus.ACTIVE);

    // Verify key in repository is encrypted
    var entity = serverKeyRepository.findById(activeKey.kid()).orElseThrow();
    assertThat(entity.getPrivateKeyEnc()).isNotNull();

    // Verify JWKS output
    Map<String, Object> jwks = serverKeyService.getJwks();
    assertThat(jwks).containsKey("keys");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
    assertThat(keys).isNotEmpty();
    assertThat(keys.get(0).get("kid")).isEqualTo(activeKey.kid());
    assertThat(keys.get(0).get("kty")).isEqualTo("EC");
    assertThat(keys.get(0).get("crv")).isEqualTo("P-256");
    assertThat(keys.get(0).get("x")).isNotNull();
    assertThat(keys.get(0).get("y")).isNotNull();
    assertThat(keys.get(0).get("status")).isEqualTo("ACTIVE");

    // Can sign and verify with active key
    ECPublicKey pubKey = serverKeyService.getActivePublicKey();
    Map<String, Object> payload = Map.of("test", "data");
    var envelope = serverKeyService.signEnvelope(payload);
    assertThat(envelope.s()).isNotBlank();
    assertThat(serverKeyService.verifyEnvelope(envelope, pubKey)).isTrue();
  }

  @Test
  @DisplayName("should rotate key by retiring current key and creating a new active key")
  void shouldRotateKey() {
    ServerKey current = serverKeyService.getActiveSigningKey();
    ServerKey rotated = serverKeyService.rotateKey();

    assertThat(rotated.kid()).isNotEqualTo(current.kid());
    assertThat(rotated.status()).isEqualTo(ServerKeyStatus.ACTIVE);

    var oldEntity = serverKeyRepository.findById(current.kid()).orElseThrow();
    assertThat(oldEntity.getStatus()).isEqualTo(ServerKeyStatus.RETIRING);

    // Both keys should be in JWKS
    Map<String, Object> jwks = serverKeyService.getJwks();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
    assertThat(keys).extracting(k -> k.get("kid")).contains(current.kid(), rotated.kid());
  }

  @Test
  @DisplayName("should automatically rotate keys past 90 days during scheduledKeyRotation")
  void shouldRotateKeysOnScheduleWhenPast90Days() {
    ServerKey current = serverKeyService.getActiveSigningKey();

    // Key is young (< 90 days) -> should not rotate
    serverKeyService.scheduledKeyRotation();
    assertThat(serverKeyService.getActiveSigningKey().kid()).isEqualTo(current.kid());

    // Age the key past 90 days via direct SQL update
    java.time.Instant pastTime =
        java.time.Instant.now().minus(91, java.time.temporal.ChronoUnit.DAYS);
    jdbcTemplate.update(
        "UPDATE server_keys SET created_at = ?, not_before = ?, not_after = ? WHERE kid = ?",
        java.sql.Timestamp.from(pastTime),
        java.sql.Timestamp.from(pastTime),
        java.sql.Timestamp.from(pastTime.plus(90, java.time.temporal.ChronoUnit.DAYS)),
        current.kid());

    serverKeyService.scheduledKeyRotation();
    ServerKey newKey = serverKeyService.getActiveSigningKey();
    assertThat(newKey.kid()).isNotEqualTo(current.kid());

    var retiredEntity = serverKeyRepository.findById(current.kid()).orElseThrow();
    assertThat(retiredEntity.getStatus()).isEqualTo(ServerKeyStatus.RETIRING);
  }
}
