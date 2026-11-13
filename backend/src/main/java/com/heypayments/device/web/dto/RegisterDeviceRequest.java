package com.heypayments.device.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record RegisterDeviceRequest(
    @NotNull(message = "publicKeyJwk is required") Map<String, String> publicKeyJwk,
    @Size(max = 60, message = "Label cannot exceed 60 characters") String label,
    @Size(max = 300, message = "User agent cannot exceed 300 characters") String userAgent) {}
