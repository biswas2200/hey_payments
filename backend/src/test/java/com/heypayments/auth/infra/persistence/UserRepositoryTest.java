package com.heypayments.auth.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.heypayments.BaseIntegrationTest;
import com.heypayments.auth.domain.model.UserRole;
import com.heypayments.auth.domain.model.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserRepositoryTest extends BaseIntegrationTest {

  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Test
  @DisplayName("should persist and retrieve user by id and phone")
  void shouldPersistAndRetrieveUser() {
    UUID userId = UUID.randomUUID();
    UserEntity user =
        UserEntity.builder()
            .id(userId)
            .phone("+919876543220")
            .email("payer.repo@example.com")
            .passwordHash("$argon2id$v=19$m=19456,t=2,p=1$fakehash")
            .role(UserRole.PAYER)
            .status(UserStatus.ACTIVE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    userRepository.save(user);

    Optional<UserEntity> byId = userRepository.findById(userId);
    assertThat(byId).isPresent();
    assertThat(byId.get().getPhone()).isEqualTo("+919876543220");
    assertThat(byId.get().getRole()).isEqualTo(UserRole.PAYER);

    Optional<UserEntity> byPhone = userRepository.findByPhone("+919876543220");
    assertThat(byPhone).isPresent();
    assertThat(byPhone.get().getId()).isEqualTo(userId);
  }

  @Test
  @DisplayName("should manage refresh token rotation and family lookup")
  void shouldManageRefreshTokens() {
    UUID userId = UUID.randomUUID();
    UserEntity user =
        UserEntity.builder()
            .id(userId)
            .phone("+919876543221")
            .passwordHash("$argon2id$v=19$m=19456,t=2,p=1$fakehash")
            .role(UserRole.PAYER)
            .status(UserStatus.ACTIVE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    userRepository.save(user);

    UUID familyId = UUID.randomUUID();
    UUID tokenId1 = UUID.randomUUID();
    RefreshTokenEntity token1 =
        RefreshTokenEntity.builder()
            .id(tokenId1)
            .userId(userId)
            .tokenHash("hash_token_1")
            .familyId(familyId)
            .expiresAt(Instant.now().plusSeconds(3600))
            .createdAt(Instant.now())
            .build();
    refreshTokenRepository.save(token1);

    Optional<RefreshTokenEntity> found = refreshTokenRepository.findByTokenHash("hash_token_1");
    assertThat(found).isPresent();
    assertThat(found.get().getFamilyId()).isEqualTo(familyId);

    // Rotate: save token2 in same family first, then link token1.replacedBy to token2
    UUID tokenId2 = UUID.randomUUID();
    RefreshTokenEntity token2 =
        RefreshTokenEntity.builder()
            .id(tokenId2)
            .userId(userId)
            .tokenHash("hash_token_2")
            .familyId(familyId)
            .expiresAt(Instant.now().plusSeconds(7200))
            .createdAt(Instant.now())
            .build();
    refreshTokenRepository.save(token2);

    token1.setRevokedAt(Instant.now());
    token1.setReplacedBy(tokenId2);
    refreshTokenRepository.save(token1);

    List<RefreshTokenEntity> familyTokens = refreshTokenRepository.findAllByFamilyId(familyId);
    assertThat(familyTokens).hasSize(2);
  }
}
