package com.heypayments.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.heypayments.BaseIntegrationTest;
import com.heypayments.auth.domain.model.UserRole;
import com.heypayments.auth.domain.model.UserStatus;
import com.heypayments.auth.infra.persistence.RefreshTokenEntity;
import com.heypayments.auth.infra.persistence.RefreshTokenRepository;
import com.heypayments.auth.infra.persistence.UserEntity;
import com.heypayments.auth.infra.persistence.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RefreshTokenServiceTest extends BaseIntegrationTest {

  @Autowired private RefreshTokenService refreshTokenService;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private UserRepository userRepository;

  @Test
  @DisplayName("should issue, rotate refresh tokens, and detect reuse to revoke token family")
  void shouldManageRefreshTokenLifecycleAndReuseDetection() {
    UUID userId = UUID.randomUUID();
    UserEntity user =
        UserEntity.builder()
            .id(userId)
            .phone("+919876543250")
            .passwordHash("$argon2id$...")
            .role(UserRole.PAYER)
            .status(UserStatus.ACTIVE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    userRepository.save(user);

    // 1. Issue initial token
    String rawToken1 = refreshTokenService.issueRefreshToken(userId);
    assertThat(rawToken1).isNotBlank();

    // 2. Rotate token
    String rawToken2 = refreshTokenService.rotateRefreshToken(rawToken1);
    assertThat(rawToken2).isNotEqualTo(rawToken1);

    // 3. Reuse detection: Attempting to use rawToken1 again must revoke entire token family!
    assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawToken1))
        .isInstanceOf(AuthTokenException.class)
        .hasMessageContaining("reuse");

    // Verify all family tokens are now revoked and were created with UUIDv7
    List<RefreshTokenEntity> allTokens = refreshTokenRepository.findAll();
    assertThat(allTokens)
        .filteredOn(t -> t.getUserId().equals(userId))
        .allMatch(
            t ->
                t.getRevokedAt() != null
                    && t.getId().version() == 7
                    && t.getFamilyId().version() == 7);

    // Attempting to use rawToken2 now must also fail because family was revoked
    assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawToken2))
        .isInstanceOf(AuthTokenException.class);
  }
}
