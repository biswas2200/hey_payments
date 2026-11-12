package com.heypayments.crypto.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class CryptoConfigTest {

  private final CryptoConfig config = new CryptoConfig();

  @Test
  @DisplayName("Uses raw 32-byte decoded key when valid base64 is provided")
  void shouldUseRawDecodedKeyWhenValidBase64() {
    byte[] expectedKey = new byte[32];
    for (int i = 0; i < 32; i++) {
      expectedKey[i] = (byte) (i + 1);
    }
    String b64Key = Base64.getEncoder().encodeToString(expectedKey);

    byte[] actualKey = config.serverMasterKey(b64Key);
    assertThat(actualKey).isEqualTo(expectedKey);
  }

  @Test
  @DisplayName("Derives 32-byte SHA-256 key when string seed is provided")
  void shouldDeriveSha256WhenStringSeed() {
    String seed = "dev-master-key-seed-32-bytes-long!";
    byte[] actualKey = config.serverMasterKey(seed);
    assertThat(actualKey).hasSize(32);
  }

  @Test
  @DisplayName("application-prod.yml mandates HP_MASTER_KEY without default fallback")
  void applicationProdMustMandateHpMasterKeyWithoutDefault() {
    Yaml yaml = new Yaml();
    try (InputStream in =
        getClass().getClassLoader().getResourceAsStream("application-prod.yml")) {
      assertThat(in).isNotNull();
      Map<String, Object> data = yaml.load(in);
      @SuppressWarnings("unchecked")
      Map<String, Object> hp = (Map<String, Object>) data.get("hp");
      assertThat(hp)
          .as("application-prod.yml must declare hp.master-key: ${HP_MASTER_KEY}")
          .isNotNull();
      assertThat(hp.get("master-key")).isEqualTo("${HP_MASTER_KEY}");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
