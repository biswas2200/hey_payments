package com.heypayments.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heypayments.crypto.domain.CryptoService;
import com.heypayments.crypto.domain.JcsCanonicalizer;
import com.heypayments.crypto.domain.SignedEnvelope;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestVectorGeneratorTest {

  private final JcsCanonicalizer jcs = new JcsCanonicalizer();
  private final CryptoService cryptoService = new CryptoService(jcs);
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("should verify all test-vectors/jcs.json canonicalization vectors")
  void verifyJcsVectors() throws Exception {
    Path testVectorsDir = Paths.get("../test-vectors").toAbsolutePath().normalize();
    if (!Files.exists(testVectorsDir)) {
      testVectorsDir = Paths.get("test-vectors").toAbsolutePath().normalize();
    }
    Path jcsPath = testVectorsDir.resolve("jcs.json");
    assertThat(Files.exists(jcsPath)).isTrue();

    List<Map<String, Object>> jcsCases =
        objectMapper.readValue(jcsPath.toFile(), new TypeReference<List<Map<String, Object>>>() {});

    for (Map<String, Object> tc : jcsCases) {
      String expected = (String) tc.get("expected");
      String actual = jcs.canonicalize(tc.get("input"));
      assertThat(actual).as("Vector: %s", tc.get("name")).isEqualTo(expected);
    }
  }

  @Test
  @DisplayName("should verify all test-vectors/sign-verify.json valid and negative cases")
  void verifySignVerifyVectors() throws Exception {
    Path testVectorsDir = Paths.get("../test-vectors").toAbsolutePath().normalize();
    if (!Files.exists(testVectorsDir)) {
      testVectorsDir = Paths.get("test-vectors").toAbsolutePath().normalize();
    }
    Path signVerifyPath = testVectorsDir.resolve("sign-verify.json");
    assertThat(Files.exists(signVerifyPath)).isTrue();

    Map<String, Object> vector =
        objectMapper.readValue(signVerifyPath.toFile(), new TypeReference<Map<String, Object>>() {});

    @SuppressWarnings("unchecked")
    Map<String, String> jwk = (Map<String, String>) vector.get("publicKeyJwk");
    ECPublicKey pubKey = cryptoService.fromJwk(jwk.get("x"), jwk.get("y"));

    @SuppressWarnings("unchecked")
    Map<String, String> wrongJwk = (Map<String, String>) vector.get("wrongPublicKeyJwk");
    ECPublicKey wrongPubKey = cryptoService.fromJwk(wrongJwk.get("x"), wrongJwk.get("y"));

    byte[] privPkcs8 = Base64.getDecoder().decode((String) vector.get("privateKeyPkcs8Base64"));
    ECPrivateKey privKey = cryptoService.privateKeyFromEncoded(privPkcs8);

    @SuppressWarnings("unchecked")
    Map<String, Object> payload = (Map<String, Object>) vector.get("payload");
    String signature = (String) vector.get("signatureP1363Base64Url");
    String canonicalJson = (String) vector.get("canonicalJson");

    // 1. Valid signature verification
    SignedEnvelope validEnvelope = new SignedEnvelope(payload, signature);
    assertThat(jcs.canonicalize(payload)).isEqualTo(canonicalJson);
    assertThat(cryptoService.verifyEnvelope(validEnvelope, pubKey)).isTrue();

    // 2. Tampered payload rejected
    @SuppressWarnings("unchecked")
    Map<String, Object> tamperedPayload = (Map<String, Object>) vector.get("tamperedPayload");
    SignedEnvelope tamperedEnvelope = new SignedEnvelope(tamperedPayload, signature);
    assertThat(cryptoService.verifyEnvelope(tamperedEnvelope, pubKey)).isFalse();

    // 3. Wrong public key rejected
    assertThat(cryptoService.verifyEnvelope(validEnvelope, wrongPubKey)).isFalse();

    // 4. Malformed signature length rejected (48 bytes instead of 64 bytes)
    String malformedSig = (String) vector.get("malformedSignatureLengthBase64Url");
    SignedEnvelope malformedSigEnvelope = new SignedEnvelope(payload, malformedSig);
    assertThat(cryptoService.verifyEnvelope(malformedSigEnvelope, pubKey)).isFalse();

    // 5. Non-integer number in protocol payload rejected
    @SuppressWarnings("unchecked")
    Map<String, Object> nonIntegerPayload = (Map<String, Object>) vector.get("nonIntegerPayload");
    assertThatThrownBy(() -> cryptoService.signEnvelope(nonIntegerPayload, privKey))
        .as("signEnvelope should reject non-integer numbers")
        .isInstanceOf(Exception.class);
    SignedEnvelope nonIntegerEnvelope = new SignedEnvelope(nonIntegerPayload, signature);
    assertThat(cryptoService.verifyEnvelope(nonIntegerEnvelope, pubKey))
        .as("verifyEnvelope should reject non-integer numbers")
        .isFalse();

    // 6. Number > 2^53 - 1 in protocol payload rejected
    @SuppressWarnings("unchecked")
    Map<String, Object> overflowPayload = (Map<String, Object>) vector.get("overflowNumberPayload");
    assertThatThrownBy(() -> cryptoService.signEnvelope(overflowPayload, privKey))
        .as("signEnvelope should reject integers > 2^53 - 1")
        .isInstanceOf(Exception.class);
    SignedEnvelope overflowEnvelope = new SignedEnvelope(overflowPayload, signature);
    assertThat(cryptoService.verifyEnvelope(overflowEnvelope, pubKey))
        .as("verifyEnvelope should reject integers > 2^53 - 1")
        .isFalse();
  }
}
