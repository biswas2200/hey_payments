package com.heypayments.crypto.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.heypayments.BaseIntegrationTest;
import com.heypayments.crypto.domain.CryptoService;
import com.heypayments.crypto.domain.JcsCanonicalizer;
import com.heypayments.crypto.domain.model.ServerKeyStatus;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ServerKeyRepositoryTest extends BaseIntegrationTest {

  @Autowired private ServerKeyRepository serverKeyRepository;
  private final CryptoService cryptoService = new CryptoService(new JcsCanonicalizer());

  @AfterEach
  void cleanUp() {
    serverKeyRepository.deleteAll();
  }

  @Test
  @DisplayName("should persist and find active server keys for JWKS and signing")
  void shouldManageServerKeys() {
    String kid = "srv-test-1";
    KeyPair kp = cryptoService.generateP256KeyPair();
    byte[] pubKey = cryptoService.toUncompressedSec1((ECPublicKey) kp.getPublic());

    ServerKeyEntity key =
        ServerKeyEntity.builder()
            .kid(kid)
            .publicKey(pubKey)
            .privateKeyEnc(new byte[] {0x56, 0x78})
            .status(ServerKeyStatus.ACTIVE)
            .notBefore(Instant.now().minusSeconds(60))
            .notAfter(Instant.now().plusSeconds(86400 * 90))
            .createdAt(Instant.now())
            .build();

    serverKeyRepository.save(key);

    Optional<ServerKeyEntity> byKid = serverKeyRepository.findById(kid);
    assertThat(byKid).isPresent();
    assertThat(byKid.get().getStatus()).isEqualTo(ServerKeyStatus.ACTIVE);

    List<ServerKeyEntity> activeKeys =
        serverKeyRepository.findAllByStatusIn(
            List.of(ServerKeyStatus.ACTIVE, ServerKeyStatus.RETIRING));
    assertThat(activeKeys).extracting(ServerKeyEntity::getKid).contains(kid);
  }
}
