package com.heypayments.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SetPinRequest(
    @NotBlank(message = "PIN is required")
        @Pattern(regexp = "^\\d{6}$", message = "PIN must be exactly 6 digits")
        String pin,
    @NotBlank(message = "Password is required to verify PIN setup") String password) {}
