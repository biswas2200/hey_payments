package com.heypayments.auth.infra.security;

import com.heypayments.auth.domain.service.PasswordHasher;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Argon2id password and PIN hashing service using Spring Security Argon2PasswordEncoder.
 * Parameters: salt length 16 bytes, hash length 32 bytes, parallelism 1, memory 19 MiB (19456 KiB), 2 iterations.
 */
@Component
public class Argon2PasswordHasher implements PasswordHasher {

  // m=19 MiB (19456 KiB), t=2, p=1, salt=16, hash=32
  private final Argon2PasswordEncoder encoder =
      new Argon2PasswordEncoder(16, 32, 1, 19456, 2);

  @Override
  public String hashPassword(String rawPassword) {
    if (rawPassword == null || rawPassword.isBlank()) {
      throw new IllegalArgumentException("Password cannot be empty");
    }
    return encoder.encode(rawPassword);
  }

  @Override
  public boolean verifyPassword(String rawPassword, String hashedPassword) {
    if (rawPassword == null || hashedPassword == null) {
      return false;
    }
    return encoder.matches(rawPassword, hashedPassword);
  }

  @Override
  public String hashPin(String rawPin) {
    if (rawPin == null || !rawPin.matches("^\\d{6}$")) {
      throw new IllegalArgumentException("PIN must be exactly 6 digits");
    }
    return encoder.encode(rawPin);
  }

  @Override
  public boolean verifyPin(String rawPin, String hashedPin) {
    if (rawPin == null || hashedPin == null) {
      return false;
    }
    return encoder.matches(rawPin, hashedPin);
  }
}
