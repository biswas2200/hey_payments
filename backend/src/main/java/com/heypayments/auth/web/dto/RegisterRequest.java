package com.heypayments.auth.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Phone number is required")
        @Pattern(
            regexp = "^\\+91[6-9][0-9]{9}$",
            message = "Phone must be a valid Indian mobile number in E.164 format (+91XXXXXXXXXX)")
        String phone,
    @Email(message = "Invalid email format") String email,
    @NotBlank(message = "Password is required")
        @Size(min = 10, max = 128, message = "Password must be between 10 and 128 characters")
        String password,
    @NotBlank(message = "Role is required")
        @Pattern(regexp = "^(PAYER|MERCHANT)$", message = "Role must be PAYER or MERCHANT")
        String role,
    @Size(max = 60, message = "Display name cannot exceed 60 characters") String displayName) {}
