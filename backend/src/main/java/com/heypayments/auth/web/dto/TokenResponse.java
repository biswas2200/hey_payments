package com.heypayments.auth.web.dto;

public record TokenResponse(String accessToken, int expiresIn, UserSummaryDto user) {}
