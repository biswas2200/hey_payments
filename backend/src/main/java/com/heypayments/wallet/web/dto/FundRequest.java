package com.heypayments.wallet.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FundRequest(
    @NotNull(message = "amount is required")
        @Min(value = 1, message = "amount must be greater than 0")
        @Max(
            value = 5_000_000,
            message = "amount cannot exceed daily funding limit of ₹50,000 (5,000,000 paise)")
        Long amount,
    String source) {}
