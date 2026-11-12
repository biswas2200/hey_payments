package com.heypayments.auth.infra.config;

import com.heypayments.auth.application.JwtTokenService;
import com.heypayments.crypto.domain.CryptoService;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig {

  @Bean
  public JwtTokenService jwtTokenService(CryptoService cryptoService, Clock clock) {
    // Generate a dedicated ES256 keypair for JWT auth tokens
    KeyPair jwtKeyPair = cryptoService.generateP256KeyPair();
    return new JwtTokenService(
        (ECPrivateKey) jwtKeyPair.getPrivate(),
        (ECPublicKey) jwtKeyPair.getPublic(),
        "jwt-auth-key-01",
        clock);
  }
}
