package com.heypayments.device.web.dto;

import java.time.Instant;
import java.util.List;

public record CrlResponse(Instant generatedAt, List<CrlEntryDto> revoked) {}
