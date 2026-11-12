package com.heypayments.auth.web;

import com.heypayments.auth.application.AuthService;
import com.heypayments.auth.application.AuthService.LoginResult;
import com.heypayments.auth.web.dto.IdResponse;
import com.heypayments.auth.web.dto.LoginRequest;
import com.heypayments.auth.web.dto.RegisterRequest;
import com.heypayments.auth.web.dto.SetPinRequest;
import com.heypayments.auth.web.dto.TokenResponse;
import com.heypayments.auth.web.dto.VerifyOtpRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "auth", description = "Authentication, registration, and session management")
public class AuthController {

  private static final String REFRESH_COOKIE_NAME = "hp_rt";
  private static final Duration REFRESH_COOKIE_MAX_AGE = Duration.ofDays(14);

  private final AuthService authService;

  @PostMapping("/register")
  @Operation(summary = "Register a new user (Payer or Merchant)")
  public ResponseEntity<IdResponse> register(@Valid @RequestBody RegisterRequest request) {
    UUID userId = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(userId));
  }

  @PostMapping("/verify-otp")
  @Operation(summary = "Verify OTP and activate user account")
  public ResponseEntity<Void> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
    authService.verifyOtp(request.phone(), request.otp());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/login")
  @Operation(summary = "Authenticate user and issue JWT and refresh token cookie")
  public ResponseEntity<TokenResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
    LoginResult result = authService.login(request);
    setRefreshCookie(response, result.refreshToken(), REFRESH_COOKIE_MAX_AGE);
    return ResponseEntity.ok(result.tokenResponse());
  }

  @PostMapping("/refresh")
  @Operation(summary = "Rotate refresh token and issue a new access token")
  public ResponseEntity<TokenResponse> refresh(
      @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
      HttpServletResponse response) {
    LoginResult result = authService.refresh(refreshToken);
    setRefreshCookie(response, result.refreshToken(), REFRESH_COOKIE_MAX_AGE);
    return ResponseEntity.ok(result.tokenResponse());
  }

  @PostMapping("/logout")
  @Operation(summary = "Logout user and revoke refresh token")
  public ResponseEntity<Void> logout(
      @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
      HttpServletResponse response) {
    authService.logout(refreshToken);
    setRefreshCookie(response, "", Duration.ZERO);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/pin")
  @PreAuthorize("hasRole('PAYER')")
  @Operation(summary = "Configure 6-digit payment PIN for offline voucher signing")
  public ResponseEntity<Void> setPin(
      @Valid @RequestBody SetPinRequest request, Principal principal) {
    UUID userId = UUID.fromString(principal.getName());
    authService.setPin(userId, request.pin(), request.password());
    return ResponseEntity.noContent().build();
  }

  private void setRefreshCookie(HttpServletResponse response, String token, Duration maxAge) {
    ResponseCookie cookie =
        ResponseCookie.from(REFRESH_COOKIE_NAME, token)
            .httpOnly(true)
            .secure(false) // Set to false in dev/local test, true in prod
            .path("/")
            .sameSite("Strict")
            .maxAge(maxAge)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }
}
