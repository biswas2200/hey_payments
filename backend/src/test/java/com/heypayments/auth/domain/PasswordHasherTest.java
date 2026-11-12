package com.heypayments.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.heypayments.auth.domain.service.PasswordHasher;
import com.heypayments.auth.infra.security.Argon2PasswordHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordHasherTest {

  private final PasswordHasher hasher = new Argon2PasswordHasher();

  @Test
  @DisplayName("should hash and verify passwords using Argon2id")
  void shouldHashAndVerifyPassword() {
    String raw = "StrongP@ssw0rd!123";
    String hash = hasher.hashPassword(raw);

    assertThat(hash).startsWith("$argon2id$");
    assertThat(hasher.verifyPassword(raw, hash)).isTrue();
    assertThat(hasher.verifyPassword("WrongPassword123", hash)).isFalse();
    assertThat(hasher.verifyPassword(null, hash)).isFalse();
    assertThat(hasher.verifyPassword(raw, null)).isFalse();

    assertThatThrownBy(() -> hasher.hashPassword(null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> hasher.hashPassword("   "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("should hash and verify 6-digit payment PIN using Argon2id")
  void shouldHashAndVerifyPin() {
    String pin = "123456";
    String hash = hasher.hashPin(pin);

    assertThat(hash).startsWith("$argon2id$");
    assertThat(hasher.verifyPin(pin, hash)).isTrue();
    assertThat(hasher.verifyPin("654321", hash)).isFalse();
    assertThat(hasher.verifyPin(null, hash)).isFalse();
    assertThat(hasher.verifyPin(pin, null)).isFalse();

    assertThatThrownBy(() -> hasher.hashPin(null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> hasher.hashPin("12345")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> hasher.hashPin("abcdef")).isInstanceOf(IllegalArgumentException.class);
  }
}
