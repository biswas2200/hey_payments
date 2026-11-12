package com.heypayments.auth.application;

import com.heypayments.auth.domain.model.User;
import com.heypayments.auth.domain.model.UserRole;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class JwtTokenService {

  private static final long ACCESS_TOKEN_VALIDITY_SECONDS = 900; // 15 minutes
  private static final String ISSUER = "heypayments";

  private final ECPrivateKey privateKey;
  private final ECPublicKey publicKey;
  private final String kid;
  private final Clock clock;

  public JwtTokenService(ECPrivateKey privateKey, ECPublicKey publicKey, String kid, Clock clock) {
    this.privateKey = privateKey;
    this.publicKey = publicKey;
    this.kid = kid;
    this.clock = clock;
  }

  public String generateAccessToken(User user, String displayName) {
    try {
      Instant now = clock.instant();
      Instant expiresAt = now.plusSeconds(ACCESS_TOKEN_VALIDITY_SECONDS);

      JWSHeader header =
          new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(kid).type(JOSEObjectType.JWT).build();

      JWTClaimsSet claimsSet =
          new JWTClaimsSet.Builder()
              .subject(user.id().toString())
              .issuer(ISSUER)
              .claim("role", user.role().name())
              .claim("displayName", displayName != null ? displayName : user.phone())
              .issueTime(Date.from(now))
              .expirationTime(Date.from(expiresAt))
              .jwtID(UUID.randomUUID().toString())
              .build();

      SignedJWT signedJWT = new SignedJWT(header, claimsSet);
      signedJWT.sign(new ECDSASigner(privateKey));
      return signedJWT.serialize();
    } catch (Exception e) {
      throw new AuthTokenException("Failed to generate ES256 JWT access token", e);
    }
  }

  public JwtClaims validateAndParseToken(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      JWSVerifier verifier = new ECDSAVerifier(publicKey);

      if (!signedJWT.verify(verifier)) {
        throw new AuthTokenException("Invalid token signature");
      }

      JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
      Instant exp =
          claimsSet.getExpirationTime() != null ? claimsSet.getExpirationTime().toInstant() : null;
      Instant now = clock.instant();

      if (exp == null || now.isAfter(exp)) {
        throw new AuthTokenException("Token expired");
      }

      UUID userId = UUID.fromString(claimsSet.getSubject());
      UserRole role = UserRole.valueOf(claimsSet.getStringClaim("role"));
      String displayName = claimsSet.getStringClaim("displayName");

      return new JwtClaims(userId, role, displayName, exp);
    } catch (AuthTokenException e) {
      throw e;
    } catch (Exception e) {
      throw new AuthTokenException("Failed to validate JWT token: " + e.getMessage(), e);
    }
  }
}
