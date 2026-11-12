package com.heypayments.crypto.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CryptoServiceTest {

  private final JcsCanonicalizer jcs = new JcsCanonicalizer();
  private final CryptoService cryptoService = new CryptoService(jcs);

  @Test
  @DisplayName("should generate valid P-256 keypair and encode uncompressed SEC1 public key")
  void shouldGenerateAndEncodeKeys() {
    KeyPair keyPair = cryptoService.generateP256KeyPair();
    assertThat(keyPair.getPublic()).isInstanceOf(ECPublicKey.class);
    assertThat(keyPair.getPrivate()).isInstanceOf(ECPrivateKey.class);

    byte[] sec1 = cryptoService.toUncompressedSec1((ECPublicKey) keyPair.getPublic());
    assertThat(sec1).hasSize(65);
    assertThat(sec1[0]).isEqualTo((byte) 0x04);

    ECPublicKey parsed = cryptoService.fromUncompressedSec1(sec1);
    assertThat(parsed.getW()).isEqualTo(((ECPublicKey) keyPair.getPublic()).getW());
  }

  @Test
  @DisplayName("should sign and verify envelope payload in IEEE P1363 format")
  void shouldSignAndVerifyEnvelope() {
    KeyPair keyPair = cryptoService.generateP256KeyPair();
    Map<String, Object> payload =
        Map.of(
            "v", 1,
            "t", "MC",
            "merchantId", "01931234-5678-7000-8000-000000000001",
            "displayName", "Ravi General Store");

    SignedEnvelope envelope =
        cryptoService.signEnvelope(payload, (ECPrivateKey) keyPair.getPrivate());

    assertThat(envelope.p()).isEqualTo(payload);
    assertThat(envelope.s()).isNotBlank();

    boolean valid = cryptoService.verifyEnvelope(envelope, (ECPublicKey) keyPair.getPublic());
    assertThat(valid).isTrue();

    // Tampering payload must fail verification
    Map<String, Object> tamperedPayload =
        Map.of(
            "v", 1,
            "t", "MC",
            "merchantId", "01931234-5678-7000-8000-000000000001",
            "displayName", "Attacker Store");
    SignedEnvelope tamperedEnvelope = new SignedEnvelope(tamperedPayload, envelope.s());
    boolean tamperedValid =
        cryptoService.verifyEnvelope(tamperedEnvelope, (ECPublicKey) keyPair.getPublic());
    assertThat(tamperedValid).isFalse();
  }

  @Test
  @DisplayName("should encrypt and decrypt private key bytes with AES-256-GCM using master key")
  void shouldEncryptAndDecryptPrivateKey() {
    byte[] masterKey = cryptoService.generateMasterKey();
    assertThat(masterKey).hasSize(32);

    KeyPair keyPair = cryptoService.generateP256KeyPair();
    byte[] privateKeyDer = keyPair.getPrivate().getEncoded();

    byte[] encrypted = cryptoService.encryptPrivateKey(privateKeyDer, masterKey);
    assertThat(encrypted).isNotEqualTo(privateKeyDer);

    byte[] decrypted = cryptoService.decryptPrivateKey(encrypted, masterKey);
    assertThat(decrypted).isEqualTo(privateKeyDer);

    // Wrong master key should fail decryption
    byte[] wrongKey = cryptoService.generateMasterKey();
    assertThatThrownBy(() -> cryptoService.decryptPrivateKey(encrypted, wrongKey))
        .isInstanceOf(CryptoException.class);
  }

  @Test
  @DisplayName("should convert EC public key to JWK map and back")
  void shouldConvertPublicKeyToJwkAndBack() {
    KeyPair keyPair = cryptoService.generateP256KeyPair();
    ECPublicKey pub = (ECPublicKey) keyPair.getPublic();

    Map<String, String> jwk = cryptoService.toJwk(pub, "srv-test-1");
    assertThat(jwk.get("kty")).isEqualTo("EC");
    assertThat(jwk.get("crv")).isEqualTo("P-256");
    assertThat(jwk.get("kid")).isEqualTo("srv-test-1");
    assertThat(jwk.get("x")).isNotBlank();
    assertThat(jwk.get("y")).isNotBlank();

    ECPublicKey fromJwk = cryptoService.fromJwk(jwk.get("x"), jwk.get("y"));
    assertThat(fromJwk.getW()).isEqualTo(pub.getW());
  }
}
