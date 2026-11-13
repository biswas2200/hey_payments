package com.heypayments.auth.web.dto;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String phone,
    String email,
    String role,
    String status,
    String displayName,
    Instant createdAt) {}
