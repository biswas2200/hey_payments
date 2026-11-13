package com.heypayments.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.heypayments.auth.domain.model.User;
import com.heypayments.auth.domain.model.UserRole;
import com.heypayments.auth.domain.model.UserStatus;
import com.heypayments.crypto.application.ServerKeyService;
import com.heypayments.crypto.domain.CryptoService;
import com.heypayments.crypto.domain.JcsCanonicalizer;
import com.heypayments.crypto.domain.model.ServerKey;
import com.heypayments.crypto.domain.model.ServerKeyPurpose;
import com.heypayments.crypto.domain.model.ServerKeyStatus;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

  private final JcsCanonicalizer jcs = new JcsCanonicalizer();
  private final CryptoService cryptoService = new CryptoService(jcs);
  private KeyPair keyPair;
  private Clock clock;
  private ServerKeyService serverKeyService;
  private JwtTokenService jwtTokenService;

  @BeforeEach
  void setUp() {
    keyPair = cryptoService.generateP256KeyPair();
    clock = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC);
    serverKeyService = mock(ServerKeyService.class);

    ServerKey mockKey =
        new ServerKey(
            "jwt-test-kid-01",
            ServerKeyPurpose.JWT,
            new byte[0],
            new byte[0],
            ServerKeyStatus.ACTIVE,
            clock.instant().minusSeconds(60),
            clock.instant().plusSeconds(86400 * 90),
            clock.instant());

    when(serverKeyService.getActiveKey(ServerKeyPurpose.JWT)).thenReturn(mockKey);
    when(serverKeyService.getPrivateKeyByKidAndPurpose("jwt-test-kid-01", ServerKeyPurpose.JWT))
        .thenReturn(Optional.of((ECPrivateKey) keyPair.getPrivate()));
    when(serverKeyService.getPublicKeyByKidAndPurpose("jwt-test-kid-01", ServerKeyPurpose.JWT))
        .thenReturn(Optional.of((ECPublicKey) keyPair.getPublic()));

    jwtTokenService = new JwtTokenService(serverKeyService, clock);
  }

  @Test
  @DisplayName("should generate and parse valid ES256 JWT access token")
  void shouldGenerateAndParseToken() {
    UUID userId = UUID.randomUUID();
    User user =
        new User(
            userId,
            "+919876543210",
            "payer@example.com",
            "hash",
            UserRole.PAYER,
            UserStatus.ACTIVE,
            null,
            clock.instant(),
            clock.instant());

    String token = jwtTokenService.generateAccessToken(user, "Ravi Kumar");
    assertThat(token).isNotBlank();

    JwtClaims claims = jwtTokenService.validateAndParseToken(token);
    assertThat(claims.userId()).isEqualTo(userId);
    assertThat(claims.role()).isEqualTo(UserRole.PAYER);
    assertThat(claims.displayName()).isEqualTo("Ravi Kumar");
    assertThat(claims.expiresAt()).isEqualTo(clock.instant().plusSeconds(900));
  }

  @Test
  @DisplayName("should reject expired token")
  void shouldRejectExpiredToken() {
    UUID userId = UUID.randomUUID();
    User user =
        new User(
            userId,
            "+919876543210",
            "payer@example.com",
            "hash",
            UserRole.PAYER,
            UserStatus.ACTIVE,
            null,
            clock.instant(),
            clock.instant());

    String token = jwtTokenService.generateAccessToken(user, "Ravi Kumar");

    // Fast-forward clock past 15 min
    Clock futureClock = Clock.fixed(clock.instant().plusSeconds(901), ZoneOffset.UTC);
    JwtTokenService futureService = new JwtTokenService(serverKeyService, futureClock);

    assertThatThrownBy(() -> futureService.validateAndParseToken(token))
        .isInstanceOf(AuthTokenException.class)
        .hasMessageContaining("expired");
  }

  @Test
  @DisplayName("should reject tampered token")
  void shouldRejectTamperedToken() {
    UUID userId = UUID.randomUUID();
    User user =
        new User(
            userId,
            "+919876543210",
            "payer@example.com",
            "hash",
            UserRole.PAYER,
            UserStatus.ACTIVE,
            null,
            clock.instant(),
            clock.instant());

    String token = jwtTokenService.generateAccessToken(user, "Ravi Kumar");
    String[] parts = token.split("\\.");
    // Tamper payload
    String tamperedToken = parts[0] + ".eyJzdWIiOiJvdGhlciJ9." + parts[2];

    assertThatThrownBy(() -> jwtTokenService.validateAndParseToken(tamperedToken))
        .isInstanceOf(AuthTokenException.class);
  }
}
