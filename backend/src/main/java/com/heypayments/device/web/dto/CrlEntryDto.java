package com.heypayments.device.web.dto;

import java.time.Instant;
import java.util.UUID;

public record CrlEntryDto(UUID deviceId, String status, Instant at) {}
