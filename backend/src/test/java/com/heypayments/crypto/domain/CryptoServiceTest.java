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

    // Invalid SEC1 byte arrays
    assertThatThrownBy(() -> cryptoService.fromUncompressedSec1(null))
        .isInstanceOf(CryptoException.class);
    assertThatThrownBy(() -> cryptoService.fromUncompressedSec1(new byte[64]))
        .isInstanceOf(CryptoException.class);
    byte[] badHeader = new byte[65];
    badHeader[0] = 0x05;
    assertThatThrownBy(() -> cryptoService.fromUncompressedSec1(badHeader))
        .isInstanceOf(CryptoException.class);
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

    // Malformed signature must fail verification
    SignedEnvelope badSigEnvelope = new SignedEnvelope(payload, "invalid-not-base64!!!");
    assertThat(cryptoService.verifyEnvelope(badSigEnvelope, (ECPublicKey) keyPair.getPublic()))
        .isFalse();
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

    // Too short ciphertext
    assertThatThrownBy(() -> cryptoService.decryptPrivateKey(null, masterKey))
        .isInstanceOf(CryptoException.class);
    assertThatThrownBy(() -> cryptoService.decryptPrivateKey(new byte[10], masterKey))
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

    // JWK without kid
    Map<String, String> jwkNoKid = cryptoService.toJwk(pub, null);
    assertThat(jwkNoKid.containsKey("kid")).isFalse();

    ECPublicKey fromJwk = cryptoService.fromJwk(jwk.get("x"), jwk.get("y"));
    assertThat(fromJwk.getW()).isEqualTo(pub.getW());
  }

  @Test
  @DisplayName("fromJwk should validate 32-byte coordinate lengths and P-256 curve membership")
  void shouldValidateJwkCoordinatesAndCurveMembership() {
    // 1. Null / blank coordinates
    assertThatThrownBy(() -> cryptoService.fromJwk(null, "yCoord"))
        .isInstanceOf(CryptoException.class)
        .hasMessageContaining("missing");
    assertThatThrownBy(() -> cryptoService.fromJwk("xCoord", ""))
        .isInstanceOf(CryptoException.class)
        .hasMessageContaining("missing");

    // 2. Coordinate not 32 bytes (e.g. 16 bytes base64url)
    String shortCoord =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);
    assertThatThrownBy(() -> cryptoService.fromJwk(shortCoord, shortCoord))
        .isInstanceOf(CryptoException.class)
        .hasMessageContaining("32 bytes");

    // 3. Coordinate point not on P-256 curve (x=1, y=1)
    byte[] oneByte = new byte[32];
    oneByte[31] = 0x01;
    String oneCoord = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(oneByte);
    assertThatThrownBy(() -> cryptoService.fromJwk(oneCoord, oneCoord))
        .isInstanceOf(CryptoException.class)
        .hasMessageContaining("curve");
  }
}
