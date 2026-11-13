package com.heypayments.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

  @Test
  @DisplayName("RefreshToken should evaluate validity, revocation, and expiration branches")
  void testRefreshTokenBranches() {
    Instant now = Instant.now();
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID familyId = UUID.randomUUID();

    RefreshToken validToken =
        new RefreshToken(
            id,
            userId,
            "hash",
            familyId,
            now.plus(7, ChronoUnit.DAYS),
            null,
            null,
            now.minus(1, ChronoUnit.HOURS));

    assertThat(validToken.isRevoked()).isFalse();
    assertThat(validToken.isExpired(now)).isFalse();
    assertThat(validToken.isValid(now)).isTrue();

    RefreshToken revokedToken =
        new RefreshToken(
            id,
            userId,
            "hash",
            familyId,
            now.plus(7, ChronoUnit.DAYS),
            now.minus(1, ChronoUnit.MINUTES),
            null,
            now.minus(1, ChronoUnit.HOURS));

    assertThat(revokedToken.isRevoked()).isTrue();
    assertThat(revokedToken.isValid(now)).isFalse();

    RefreshToken expiredToken =
        new RefreshToken(
            id,
            userId,
            "hash",
            familyId,
            now.minus(1, ChronoUnit.MINUTES),
            null,
            null,
            now.minus(7, ChronoUnit.DAYS));

    assertThat(expiredToken.isExpired(now)).isTrue();
    assertThat(expiredToken.isValid(now)).isFalse();
  }
}
