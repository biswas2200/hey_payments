package com.heypayments.auth.domain.event;

import com.heypayments.auth.domain.model.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(UUID userId, UserRole role, Instant registeredAt) {}
