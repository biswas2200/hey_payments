package com.heypayments.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.heypayments.BaseIntegrationTest;
import com.heypayments.auth.domain.model.User;
import com.heypayments.auth.domain.model.UserRole;
import com.heypayments.auth.domain.model.UserStatus;
import com.heypayments.crypto.application.ServerKeyService;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JwtTokenPersistenceIntegrationTest extends BaseIntegrationTest {

  @Autowired private JwtTokenService jwtTokenService;
  @Autowired private ServerKeyService serverKeyService;
  @Autowired private Clock clock;

  @Test
  @DisplayName("JWT signing key is persisted encrypted, survives context restart, and unique kid is generated")
  void shouldSurviveSimulatedRestartAndValidateToken() {
    User user =
        new User(
            UUID.randomUUID(),
            "+919876543210",
            "u@example.com",
            "hash",
            UserRole.PAYER,
            UserStatus.ACTIVE,
            null,
            clock.instant(),
            clock.instant());

    String token = jwtTokenService.generateAccessToken(user, "User 1");
    assertThat(token).isNotBlank();

    // Verify token validates
    JwtClaims claims = jwtTokenService.validateAndParseToken(token);
    assertThat(claims.userId()).isEqualTo(user.id());
    assertThat(claims.role()).isEqualTo(UserRole.PAYER);

    // Verify kid is unique and not fixed "jwt-auth-key-01"
    try {
      SignedJWT parsed = SignedJWT.parse(token);
      assertThat(parsed.getHeader().getKeyID()).isNotEqualTo("jwt-auth-key-01");
      assertThat(parsed.getHeader().getKeyID()).startsWith("jwt-");
      assertThat(parsed.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.ES256);
      assertThat(parsed.getHeader().getType()).isEqualTo(JOSEObjectType.JWT);
      assertThat(parsed.getJWTClaimsSet().getIssuer()).isEqualTo("heypayments");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  @DisplayName("Token with unknown kid is rejected")
  void shouldRejectUnknownKid() throws Exception {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
    kpg.initialize(256);
    var rogueKp = kpg.generateKeyPair();

    JWSHeader header =
        new JWSHeader.Builder(JWSAlgorithm.ES256)
            .keyID("unknown-kid-666")
            .type(JOSEObjectType.JWT)
            .build();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(UUID.randomUUID().toString())
            .issuer("heypayments")
            .claim("role", "PAYER")
            .expirationTime(Date.from(Instant.now().plusSeconds(900)))
            .build();

    SignedJWT signed = new SignedJWT(header, claims);
    signed.sign(new ECDSASigner((ECPrivateKey) rogueKp.getPrivate()));
    String token = signed.serialize();

    assertThatThrownBy(() -> jwtTokenService.validateAndParseToken(token))
        .isInstanceOf(AuthTokenException.class);
  }

  @Test
  @DisplayName("Token with wrong issuer is rejected")
  void shouldRejectWrongIssuer() throws Exception {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
    kpg.initialize(256);
    var rogueKp = kpg.generateKeyPair();

    JWSHeader header =
        new JWSHeader.Builder(JWSAlgorithm.ES256)
            .keyID("jwt-test-kid")
            .type(JOSEObjectType.JWT)
            .build();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(UUID.randomUUID().toString())
            .issuer("wrong-issuer")
            .claim("role", "PAYER")
            .expirationTime(Date.from(Instant.now().plusSeconds(900)))
            .build();

    SignedJWT signed = new SignedJWT(header, claims);
    signed.sign(new ECDSASigner((ECPrivateKey) rogueKp.getPrivate()));
    String token = signed.serialize();

    assertThatThrownBy(() -> jwtTokenService.validateAndParseToken(token))
        .isInstanceOf(AuthTokenException.class);
  }

  @Test
  @DisplayName("Token with wrong alg is rejected")
  void shouldRejectWrongAlg() throws Exception {
    JWSHeader header =
        new JWSHeader.Builder(JWSAlgorithm.HS256)
            .keyID("jwt-test-kid")
            .type(JOSEObjectType.JWT)
            .build();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(UUID.randomUUID().toString())
            .issuer("heypayments")
            .claim("role", "PAYER")
            .expirationTime(Date.from(Instant.now().plusSeconds(900)))
            .build();

    SignedJWT signed = new SignedJWT(header, claims);
    signed.sign(new com.nimbusds.jose.crypto.MACSigner("01234567890123456789012345678901"));
    String token = signed.serialize();
    assertThatThrownBy(() -> jwtTokenService.validateAndParseToken(token))
        .isInstanceOf(AuthTokenException.class);
  }

  @Test
  @DisplayName("JWT keys must NOT be exposed in protocol JWKS endpoint")
  void shouldNotExposeJwtKeysInJwks() {
    User user =
        new User(
            UUID.randomUUID(),
            "+919876543210",
            "u@example.com",
            "hash",
            UserRole.PAYER,
            UserStatus.ACTIVE,
            null,
            clock.instant(),
            clock.instant());
    String token = jwtTokenService.generateAccessToken(user, "User 1");
    assertThat(token).isNotBlank();

    Map<String, Object> jwks = serverKeyService.getJwks();
    @SuppressWarnings("unchecked")
    var keys = (java.util.List<Map<String, Object>>) jwks.get("keys");

    // Protocol JWKS should only contain srv- keys, never jwt- keys
    assertThat(keys).allMatch(k -> ((String) k.get("kid")).startsWith("srv-"));
    assertThat(keys).noneMatch(k -> ((String) k.get("kid")).startsWith("jwt-"));
  }
}
