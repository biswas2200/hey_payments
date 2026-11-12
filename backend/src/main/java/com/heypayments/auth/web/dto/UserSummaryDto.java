package com.heypayments.auth.web.dto;

import java.util.UUID;

public record UserSummaryDto(UUID id, String role, String displayName) {}
