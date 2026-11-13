package com.heypayments.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  @DisplayName("User isActive should evaluate ACTIVE status correctly")
  void testUserActive() {
    User active =
        new User(
            UUID.randomUUID(),
            "+919876543210",
            "u@hey",
            "hash",
            UserRole.PAYER,
            UserStatus.ACTIVE,
            null,
            Instant.now(),
            Instant.now());
    assertThat(active.isActive()).isTrue();

    User pending =
        new User(
            UUID.randomUUID(),
            "+919876543210",
            "u@hey",
            "hash",
            UserRole.PAYER,
            UserStatus.PENDING_VERIFICATION,
            null,
            Instant.now(),
            Instant.now());
    assertThat(pending.isActive()).isFalse();

    User locked =
        new User(
            UUID.randomUUID(),
            "+919876543210",
            "u@hey",
            "hash",
            UserRole.PAYER,
            UserStatus.LOCKED,
            null,
            Instant.now(),
            Instant.now());
    assertThat(locked.isActive()).isFalse();
  }
}
