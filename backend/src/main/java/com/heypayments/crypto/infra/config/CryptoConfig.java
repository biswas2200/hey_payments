package com.heypayments.crypto.infra.config;

import com.heypayments.crypto.domain.CryptoService;
import com.heypayments.crypto.domain.JcsCanonicalizer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoConfig {

  @Bean
  public JcsCanonicalizer jcsCanonicalizer() {
    return new JcsCanonicalizer();
  }

  @Bean
  public CryptoService cryptoService(JcsCanonicalizer jcsCanonicalizer) {
    return new CryptoService(jcsCanonicalizer);
  }

  @Bean(name = "serverMasterKey")
  public byte[] serverMasterKey(@Value("${hp.master-key}") String rawMasterKey) {
    try {
      // If it is valid base64 and decodes to 32 bytes, use directly
      byte[] decoded = Base64.getDecoder().decode(rawMasterKey);
      if (decoded.length == 32) {
        return decoded;
      }
    } catch (Exception ignored) {
      // Fall through to SHA-256 derivation
    }
    // Derive exactly 32 bytes via SHA-256 of the seed
    try {
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      return sha256.digest(rawMasterKey.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to initialize master key", e);
    }
  }
}
