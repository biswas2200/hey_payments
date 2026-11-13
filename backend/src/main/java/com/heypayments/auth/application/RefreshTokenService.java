package com.heypayments.auth.application;

import com.heypayments.auth.infra.persistence.RefreshTokenEntity;
import com.heypayments.auth.infra.persistence.RefreshTokenRepository;
import com.heypayments.shared.infra.id.UuidV7Generator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private static final long REFRESH_TOKEN_VALIDITY_DAYS = 14;

  private final RefreshTokenRepository refreshTokenRepository;
  private final Clock clock;
  private final SecureRandom secureRandom = new SecureRandom();

  @Transactional
  public String issueRefreshToken(UUID userId) {
    UUID familyId = UuidV7Generator.generate();
    return createTokenRecord(userId, familyId);
  }

  @Transactional(noRollbackFor = AuthTokenException.class)
  public String rotateRefreshToken(String rawToken) {
    String tokenHash = hashToken(rawToken);
    RefreshTokenEntity current =
        refreshTokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(() -> new AuthTokenException("Invalid refresh token"));

    Instant now = clock.instant();

    // Reuse detection: if a revoked token is presented, revoke the entire token family!
    if (current.getRevokedAt() != null) {
      log.warn(
          "Refresh token reuse detected for user {} family {}. Revoking all family tokens.",
          current.getUserId(),
          current.getFamilyId());
      refreshTokenRepository.revokeAllByFamilyId(current.getFamilyId());
      throw new AuthTokenException("Refresh token reuse detected; token family has been revoked");
    }

    if (now.isAfter(current.getExpiresAt())) {
      throw new AuthTokenException("Refresh token expired");
    }

    // Issue new token in same family
    UUID newTokenId = UuidV7Generator.generate();
    String newRawToken = generateSecureTokenString();
    String newTokenHash = hashToken(newRawToken);
    Instant newExpiresAt = now.plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS);

    RefreshTokenEntity nextToken =
        RefreshTokenEntity.builder()
            .id(newTokenId)
            .userId(current.getUserId())
            .tokenHash(newTokenHash)
            .familyId(current.getFamilyId())
            .expiresAt(newExpiresAt)
            .createdAt(now)
            .build();
    refreshTokenRepository.save(nextToken);

    // Mark current token as rotated
    current.setRevokedAt(now);
    current.setReplacedBy(newTokenId);
    refreshTokenRepository.save(current);

    return newRawToken;
  }

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public void revokeFamily(UUID familyId) {
    refreshTokenRepository.revokeAllByFamilyId(familyId);
  }

  @Transactional(readOnly = true)
  public UUID getUserIdFromValidToken(String rawToken) {
    String tokenHash = hashToken(rawToken);
    RefreshTokenEntity token =
        refreshTokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(() -> new AuthTokenException("Invalid refresh token"));

    Instant now = clock.instant();
    if (token.getRevokedAt() != null || now.isAfter(token.getExpiresAt())) {
      throw new AuthTokenException("Refresh token is revoked or expired");
    }
    return token.getUserId();
  }

  @Transactional
  public void revokeRefreshToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return;
    }
    String tokenHash = hashToken(rawToken);
    refreshTokenRepository
        .findByTokenHash(tokenHash)
        .ifPresent(
            token -> {
              token.setRevokedAt(clock.instant());
              refreshTokenRepository.save(token);
            });
  }

  private String createTokenRecord(UUID userId, UUID familyId) {
    Instant now = clock.instant();
    Instant expiresAt = now.plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS);

    String rawToken = generateSecureTokenString();
    String tokenHash = hashToken(rawToken);

    RefreshTokenEntity entity =
        RefreshTokenEntity.builder()
            .id(UuidV7Generator.generate())
            .userId(userId)
            .tokenHash(tokenHash)
            .familyId(familyId)
            .expiresAt(expiresAt)
            .createdAt(now)
            .build();

    refreshTokenRepository.save(entity);
    return rawToken;
  }

  private String generateSecureTokenString() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hashToken(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm missing", e);
    }
  }
}
