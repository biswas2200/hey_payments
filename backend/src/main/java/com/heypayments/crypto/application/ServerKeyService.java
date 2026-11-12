package com.heypayments.crypto.application;

import com.heypayments.crypto.domain.CryptoService;
import com.heypayments.crypto.domain.SignedEnvelope;
import com.heypayments.crypto.domain.model.ServerKey;
import com.heypayments.crypto.domain.model.ServerKeyStatus;
import com.heypayments.crypto.infra.persistence.ServerKeyEntity;
import com.heypayments.crypto.infra.persistence.ServerKeyRepository;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerKeyService {

  private final ServerKeyRepository serverKeyRepository;
  private final CryptoService cryptoService;
  private final Clock clock;

  @Qualifier("serverMasterKey")
  private final byte[] masterKey;

  @Transactional
  public synchronized ServerKey getActiveSigningKey() {
    Instant now = clock.instant();
    Optional<ServerKeyEntity> activeEntity =
        serverKeyRepository.findFirstByStatusOrderByCreatedAtDesc(ServerKeyStatus.ACTIVE);

    if (activeEntity.isPresent() && !now.isAfter(activeEntity.get().getNotAfter())) {
      return toDomain(activeEntity.get());
    }

    log.info("No active server signing key found or key expired; generating a new active key");
    return generateAndSaveKey(ServerKeyStatus.ACTIVE);
  }

  @Transactional
  public synchronized ServerKey rotateKey() {
    Instant now = clock.instant();
    Optional<ServerKeyEntity> activeEntity =
        serverKeyRepository.findFirstByStatusOrderByCreatedAtDesc(ServerKeyStatus.ACTIVE);

    activeEntity.ifPresent(
        entity -> {
          entity.setStatus(ServerKeyStatus.RETIRING);
          serverKeyRepository.save(entity);
          log.info("Retired server signing key kid={}", entity.getKid());
        });

    return generateAndSaveKey(ServerKeyStatus.ACTIVE);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getJwks() {
    List<ServerKeyEntity> keys =
        serverKeyRepository.findAllByStatusIn(
            List.of(ServerKeyStatus.ACTIVE, ServerKeyStatus.RETIRING));

    List<Map<String, Object>> jwkList = new ArrayList<>();
    for (ServerKeyEntity entity : keys) {
      ECPublicKey pubKey = cryptoService.fromUncompressedSec1(entity.getPublicKey());
      Map<String, String> baseJwk = cryptoService.toJwk(pubKey, entity.getKid());

      Map<String, Object> jwkEntry = new LinkedHashMap<>(baseJwk);
      jwkEntry.put("status", entity.getStatus().name());
      jwkList.add(jwkEntry);
    }

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("keys", jwkList);
    return response;
  }

  @Transactional
  public SignedEnvelope signEnvelope(Map<String, Object> payload) {
    ServerKey activeKey = getActiveSigningKey();
    ServerKeyEntity entity =
        serverKeyRepository
            .findById(activeKey.kid())
            .orElseThrow(() -> new IllegalStateException("Active key not found in repository"));

    byte[] privateKeyDer = cryptoService.decryptPrivateKey(entity.getPrivateKeyEnc(), masterKey);
    ECPrivateKey privateKey = cryptoService.privateKeyFromEncoded(privateKeyDer);

    return cryptoService.signEnvelope(payload, privateKey);
  }

  public boolean verifyEnvelope(SignedEnvelope envelope, ECPublicKey publicKey) {
    return cryptoService.verifyEnvelope(envelope, publicKey);
  }

  @Transactional(readOnly = true)
  public ECPublicKey getActivePublicKey() {
    ServerKey activeKey = getActiveSigningKey();
    return cryptoService.fromUncompressedSec1(activeKey.publicKey());
  }

  @Transactional(readOnly = true)
  public Optional<ECPublicKey> getPublicKeyByKid(String kid) {
    return serverKeyRepository
        .findById(kid)
        .map(entity -> cryptoService.fromUncompressedSec1(entity.getPublicKey()));
  }

  private ServerKey generateAndSaveKey(ServerKeyStatus status) {
    Instant now = clock.instant();
    Instant expiresAt = now.plus(90, ChronoUnit.DAYS);

    KeyPair keyPair = cryptoService.generateP256KeyPair();
    byte[] sec1Pub = cryptoService.toUncompressedSec1((ECPublicKey) keyPair.getPublic());
    byte[] encryptedPriv =
        cryptoService.encryptPrivateKey(keyPair.getPrivate().getEncoded(), masterKey);

    String kid = "srv-" + UUID.randomUUID().toString().substring(0, 8);

    ServerKeyEntity entity =
        ServerKeyEntity.builder()
            .kid(kid)
            .publicKey(sec1Pub)
            .privateKeyEnc(encryptedPriv)
            .status(status)
            .notBefore(now.minus(1, ChronoUnit.MINUTES)) // 1 min clock skew grace
            .notAfter(expiresAt)
            .createdAt(now)
            .build();

    ServerKeyEntity saved = serverKeyRepository.save(entity);
    log.info("Saved new server signing key kid={} status={} expiresAt={}", kid, status, expiresAt);
    return toDomain(saved);
  }

  private ServerKey toDomain(ServerKeyEntity entity) {
    return new ServerKey(
        entity.getKid(),
        entity.getPublicKey(),
        entity.getPrivateKeyEnc(),
        entity.getStatus(),
        entity.getNotBefore(),
        entity.getNotAfter(),
        entity.getCreatedAt());
  }
}
