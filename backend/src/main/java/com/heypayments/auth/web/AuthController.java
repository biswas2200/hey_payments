package com.heypayments.auth.web;

import com.heypayments.auth.application.AuthService;
import com.heypayments.auth.application.AuthService.LoginResult;
import com.heypayments.auth.web.dto.IdResponse;
import com.heypayments.auth.web.dto.LoginRequest;
import com.heypayments.auth.web.dto.RegisterRequest;
import com.heypayments.auth.web.dto.SetPinRequest;
import com.heypayments.auth.web.dto.TokenResponse;
import com.heypayments.auth.web.dto.VerifyOtpRequest;
import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
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
  @PreAuthorize("permitAll()")
  @Operation(summary = "Register a new user (Payer or Merchant)")
  public ResponseEntity<IdResponse> register(@Valid @RequestBody RegisterRequest request) {
    UUID userId = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(userId));
  }

  @PostMapping("/verify-otp")
  @PreAuthorize("permitAll()")
  @Operation(summary = "Verify OTP and activate user account")
  public ResponseEntity<Void> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
    authService.verifyOtp(request.phone(), request.otp());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/login")
  @PreAuthorize("permitAll()")
  @Operation(summary = "Authenticate user and issue JWT and refresh token cookie")
  public ResponseEntity<TokenResponse> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse response) {
    LoginResult result = authService.login(request);
    setRefreshCookie(httpRequest, response, result.refreshToken(), REFRESH_COOKIE_MAX_AGE);
    return ResponseEntity.ok(result.tokenResponse());
  }

  @PostMapping("/refresh")
  @PreAuthorize("permitAll()")
  @Operation(summary = "Rotate refresh token and issue a new access token")
  public ResponseEntity<TokenResponse> refresh(
      @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
      HttpServletRequest httpRequest,
      HttpServletResponse response) {
    validateOriginOrReferer(httpRequest);
    LoginResult result = authService.refresh(refreshToken);
    setRefreshCookie(httpRequest, response, result.refreshToken(), REFRESH_COOKIE_MAX_AGE);
    return ResponseEntity.ok(result.tokenResponse());
  }

  @PostMapping("/logout")
  @PreAuthorize("permitAll()")
  @Operation(summary = "Logout user and revoke refresh token")
  public ResponseEntity<Void> logout(
      @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
      HttpServletRequest httpRequest,
      HttpServletResponse response) {
    authService.logout(refreshToken);
    setRefreshCookie(httpRequest, response, "", Duration.ZERO);
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

  private static final Set<String> ALLOWED_ORIGIN_HOSTS = Set.of("localhost", "127.0.0.1", "nginx");

  private void validateOriginOrReferer(HttpServletRequest request) {
    String origin = request.getHeader("Origin");
    String referer = request.getHeader("Referer");
    String candidate = (origin != null && !origin.isBlank()) ? origin : referer;

    if (candidate != null && !candidate.isBlank()) {
      try {
        URI uri = URI.create(candidate);
        String host = uri.getHost();
        if (host == null) {
          throw new DomainException(ErrorCode.FORBIDDEN, "Invalid origin header");
        }
        String serverName = request.getServerName();
        boolean allowed =
            host.equalsIgnoreCase(serverName)
                || ALLOWED_ORIGIN_HOSTS.contains(host.toLowerCase(Locale.ROOT));
        if (!allowed) {
          throw new DomainException(ErrorCode.FORBIDDEN, "Cross-origin refresh forbidden");
        }
      } catch (IllegalArgumentException e) {
        throw new DomainException(ErrorCode.FORBIDDEN, "Malformed origin header");
      }
    }
  }

  private void setRefreshCookie(
      HttpServletRequest request, HttpServletResponse response, String token, Duration maxAge) {
    boolean secure =
        request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    ResponseCookie cookie =
        ResponseCookie.from(REFRESH_COOKIE_NAME, token)
            .httpOnly(true)
            .secure(secure)
            .path("/api/v1/auth")
            .sameSite("Strict")
            .maxAge(maxAge)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }
}
