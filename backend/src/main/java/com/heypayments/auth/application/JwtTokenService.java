package com.heypayments.auth.application;

import com.heypayments.auth.domain.model.User;
import com.heypayments.auth.domain.model.UserRole;
import com.heypayments.crypto.application.ServerKeyService;
import com.heypayments.crypto.domain.model.ServerKey;
import com.heypayments.crypto.domain.model.ServerKeyPurpose;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtTokenService {

  private static final long ACCESS_TOKEN_VALIDITY_SECONDS = 900; // 15 minutes
  private static final String ISSUER = "heypayments";

  private final ServerKeyService serverKeyService;
  private final Clock clock;

  public String generateAccessToken(User user, String displayName) {
    try {
      ServerKey activeKey = serverKeyService.getActiveKey(ServerKeyPurpose.JWT);
      ECPrivateKey privateKey =
          serverKeyService
              .getPrivateKeyByKidAndPurpose(activeKey.kid(), ServerKeyPurpose.JWT)
              .orElseThrow(
                  () ->
                      new AuthTokenException(
                          "Private key not found for active JWT kid: " + activeKey.kid()));

      Instant now = clock.instant();
      Instant expiresAt = now.plusSeconds(ACCESS_TOKEN_VALIDITY_SECONDS);

      JWSHeader header =
          new JWSHeader.Builder(JWSAlgorithm.ES256)
              .keyID(activeKey.kid())
              .type(JOSEObjectType.JWT)
              .build();

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
    } catch (AuthTokenException e) {
      throw e;
    } catch (Exception e) {
      throw new AuthTokenException("Failed to generate ES256 JWT access token", e);
    }
  }

  public JwtClaims validateAndParseToken(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      JWSHeader header = signedJWT.getHeader();

      // Enforce alg=ES256 and typ=JWT per doc 08
      if (!JWSAlgorithm.ES256.equals(header.getAlgorithm())) {
        throw new AuthTokenException("Invalid token algorithm: " + header.getAlgorithm());
      }
      if (header.getType() == null || !"JWT".equalsIgnoreCase(header.getType().getType())) {
        throw new AuthTokenException("Invalid token type");
      }

      String kid = header.getKeyID();
      if (kid == null || kid.isBlank()) {
        throw new AuthTokenException("Missing key ID (kid) in token header");
      }

      ECPublicKey publicKey =
          serverKeyService
              .getPublicKeyByKidAndPurpose(kid, ServerKeyPurpose.JWT)
              .orElseThrow(
                  () -> new AuthTokenException("Unknown or retired/revoked key ID: " + kid));

      JWSVerifier verifier = new ECDSAVerifier(publicKey);
      if (!signedJWT.verify(verifier)) {
        throw new AuthTokenException("Invalid token signature");
      }

      JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

      // Enforce iss=heypayments per doc 08
      if (!ISSUER.equals(claimsSet.getIssuer())) {
        throw new AuthTokenException("Invalid token issuer: " + claimsSet.getIssuer());
      }

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
