package com.heypayments.auth.application;

import com.heypayments.auth.domain.model.UserRole;
import java.time.Instant;
import java.util.UUID;

public record JwtClaims(
    UUID userId,
    UserRole role,
    String displayName,
    Instant expiresAt) {}
