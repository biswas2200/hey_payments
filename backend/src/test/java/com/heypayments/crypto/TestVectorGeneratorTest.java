package com.heypayments.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.heypayments.crypto.domain.CryptoService;
import com.heypayments.crypto.domain.JcsCanonicalizer;
import com.heypayments.crypto.domain.SignedEnvelope;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestVectorGeneratorTest {

  private final JcsCanonicalizer jcs = new JcsCanonicalizer();
  private final CryptoService cryptoService = new CryptoService(jcs);
  private final ObjectMapper objectMapper =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  @Test
  @DisplayName("should generate and verify test-vectors/jcs.json and test-vectors/sign-verify.json")
  void generateAndVerifyVectors() throws Exception {
    Path testVectorsDir = Paths.get("../test-vectors").toAbsolutePath().normalize();
    if (!Files.exists(testVectorsDir)) {
      testVectorsDir = Paths.get("test-vectors").toAbsolutePath().normalize();
    }
    assertThat(Files.exists(testVectorsDir)).isTrue();

    // 1. Generate jcs.json
    List<Map<String, Object>> jcsCases =
        List.of(
            Map.of(
                "name", "key_sorting",
                "input", Map.of("z", 100, "a", "first", "m", true),
                "expected", "{\"a\":\"first\",\"m\":true,\"z\":100}"),
            Map.of(
                "name", "nested_structure",
                "input", Map.of("b", List.of(3, 2, 1), "a", Map.of("y", 2, "x", 1)),
                "expected", "{\"a\":{\"x\":1,\"y\":2},\"b\":[3,2,1]}"),
            Map.of(
                "name", "escapes_and_controls",
                "input", Map.of("msg", "Line 1\nLine 2\tTab \"Quotes\" \\ Backslash"),
                "expected", "{\"msg\":\"Line 1\\nLine 2\\tTab \\\"Quotes\\\" \\\\ Backslash\"}"),
            Map.of(
                "name",
                "money_voucher_payload",
                "input",
                Map.of(
                    "v", 1,
                    "t", "VC",
                    "voucherId", "01931234-5678-7000-8000-000000000002",
                    "amount", 12000,
                    "cumSpent", 12000,
                    "counter", 1),
                "expected",
                "{\"amount\":12000,\"counter\":1,\"cumSpent\":12000,\"t\":\"VC\",\"v\":1,\"voucherId\":\"01931234-5678-7000-8000-000000000002\"}"));

    for (Map<String, Object> tc : jcsCases) {
      String expected = (String) tc.get("expected");
      String actual = jcs.canonicalize(tc.get("input"));
      assertThat(actual).isEqualTo(expected);
    }

    Path jcsPath = testVectorsDir.resolve("jcs.json");
    objectMapper.writeValue(jcsPath.toFile(), jcsCases);
    assertThat(Files.exists(jcsPath)).isTrue();

    // 2. Generate sign-verify.json with a deterministic EC keypair
    KeyPair keyPair = cryptoService.generateP256KeyPair();
    ECPublicKey pub = (ECPublicKey) keyPair.getPublic();
    ECPrivateKey priv = (ECPrivateKey) keyPair.getPrivate();

    byte[] sec1 = cryptoService.toUncompressedSec1(pub);
    String sec1Base64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(sec1);
    Map<String, String> jwk = cryptoService.toJwk(pub, "srv-test-key-01");
    String privPkcs8Base64 = Base64.getEncoder().encodeToString(priv.getEncoded());

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("v", 1);
    payload.put("t", "MC");
    payload.put("merchantId", "01931234-5678-7000-8000-000000000001");
    payload.put("deviceId", "01931234-5678-7000-8000-000000000009");
    payload.put("displayName", "Ravi General Store");
    payload.put("issuedAt", "2026-11-12T10:00:00Z");

    SignedEnvelope envelope = cryptoService.signEnvelope(payload, priv);
    assertThat(cryptoService.verifyEnvelope(envelope, pub)).isTrue();

    Map<String, Object> signVerifyData = new LinkedHashMap<>();
    signVerifyData.put(
        "description", "Cross-language ECDSA P-256 / SHA-256 / IEEE P1363 test vector");
    signVerifyData.put("privateKeyPkcs8Base64", privPkcs8Base64);
    signVerifyData.put("publicKeySec1Base64Url", sec1Base64Url);
    signVerifyData.put("publicKeyJwk", jwk);
    signVerifyData.put("payload", payload);
    signVerifyData.put("canonicalJson", jcs.canonicalize(payload));
    signVerifyData.put("signatureP1363Base64Url", envelope.s());

    // Tampered payload and tampered signature cases
    Map<String, Object> tamperedPayload = new LinkedHashMap<>(payload);
    tamperedPayload.put("displayName", "Malicious Store");
    signVerifyData.put("tamperedPayload", tamperedPayload);

    Path signVerifyPath = testVectorsDir.resolve("sign-verify.json");
    objectMapper.writeValue(signVerifyPath.toFile(), signVerifyData);
    assertThat(Files.exists(signVerifyPath)).isTrue();
  }
}
