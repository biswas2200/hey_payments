package com.heypayments.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.heypayments.auth.domain.model.UserRole;
import com.heypayments.auth.domain.model.UserStatus;
import com.heypayments.auth.domain.service.PasswordHasher;
import com.heypayments.auth.infra.persistence.UserEntity;
import com.heypayments.auth.infra.persistence.UserRepository;
import com.heypayments.auth.web.dto.LoginRequest;
import com.heypayments.auth.web.dto.RegisterRequest;
import com.heypayments.auth.web.dto.UserProfileResponse;
import com.heypayments.merchant.application.MerchantService;
import com.heypayments.merchant.domain.model.Merchant;
import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuthServiceTest {

  private UserRepository userRepository;
  private MerchantService merchantService;
  private PasswordHasher passwordHasher;
  private JwtTokenService jwtTokenService;
  private RefreshTokenService refreshTokenService;
  private OtpService otpService;
  private PhoneRateLimiter phoneRateLimiter;
  private org.springframework.context.ApplicationEventPublisher eventPublisher;
  private Clock clock;
  private AuthService authService;

  private final Instant now = Instant.parse("2026-11-12T10:00:00Z");

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    merchantService = mock(MerchantService.class);
    passwordHasher = mock(PasswordHasher.class);
    jwtTokenService = mock(JwtTokenService.class);
    refreshTokenService = mock(RefreshTokenService.class);
    otpService = mock(OtpService.class);
    phoneRateLimiter = mock(PhoneRateLimiter.class);
    eventPublisher = mock(org.springframework.context.ApplicationEventPublisher.class);
    clock = Clock.fixed(now, ZoneId.of("UTC"));

    authService =
        new AuthService(
            userRepository,
            merchantService,
            passwordHasher,
            jwtTokenService,
            refreshTokenService,
            otpService,
            phoneRateLimiter,
            eventPublisher,
            clock);
  }

  @Test
  @DisplayName("register should succeed for payer and fail for duplicate phone")
  void testRegisterPayer() {
    RegisterRequest req =
        new RegisterRequest("+919876543210", "payer@example.com", "Password123!", "PAYER", null);

    when(userRepository.existsByPhone(req.phone())).thenReturn(false);
    when(passwordHasher.hashPassword(req.password())).thenReturn("$argon2id$hashed");

    UUID userId = authService.register(req);
    assertThat(userId).isNotNull();
    assertThat(userId.version()).isEqualTo(7);
    ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
    verify(userRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
    verify(otpService).generateAndSendOtp(eq(req.phone()), eq(req.email()));

    // Conflict branch
    when(userRepository.existsByPhone(req.phone())).thenReturn(true);
    assertThatThrownBy(() -> authService.register(req))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.CONFLICT);
  }

  @Test
  @DisplayName("register should succeed for merchant with and without handle collision")
  void testRegisterMerchant() {
    RegisterRequest reqWithDisplayName =
        new RegisterRequest(
            "+919876543211", null, "Password123!", "MERCHANT", "Ravi General Store");

    when(userRepository.existsByPhone(anyString())).thenReturn(false);
    when(passwordHasher.hashPassword(anyString())).thenReturn("$argon2id$hashed");
    when(merchantService.existsByHandle("ravi.general.store@hey")).thenReturn(true);

    UUID mId1 = authService.register(reqWithDisplayName);
    assertThat(mId1).isNotNull();
    verify(merchantService)
        .createMerchant(
            any(UUID.class), eq("Ravi General Store"), anyString(), eq("GENERAL"), eq(1_000_000L));

    // Merchant with null display name
    RegisterRequest reqNullName =
        new RegisterRequest("+919876543212", null, "Password123!", "MERCHANT", null);
    when(merchantService.existsByHandle(anyString())).thenReturn(false);
    UUID mId2 = authService.register(reqNullName);
    assertThat(mId2).isNotNull();
  }

  @Test
  @DisplayName("verifyOtp should activate user or throw on invalid OTP")
  void testVerifyOtp() {
    String phone = "+919876543210";
    when(otpService.verifyAndConsumeOtp(phone, "000000")).thenReturn(false);
    assertThatThrownBy(() -> authService.verifyOtp(phone, "000000"))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.VALIDATION_FAILED);

    when(otpService.verifyAndConsumeOtp(phone, "123456")).thenReturn(true);
    when(userRepository.findByPhone(phone)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> authService.verifyOtp(phone, "123456"))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.NOT_FOUND);

    UserEntity user =
        UserEntity.builder()
            .id(UUID.randomUUID())
            .phone(phone)
            .status(UserStatus.PENDING_VERIFICATION)
            .role(UserRole.PAYER)
            .build();
    when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));

    authService.verifyOtp(phone, "123456");
    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    verify(userRepository).save(user);

    // Locked user cannot be unlocked via OTP
    UserEntity fraudLockedUser =
        UserEntity.builder()
            .id(UUID.randomUUID())
            .phone(phone)
            .status(UserStatus.LOCKED)
            .role(UserRole.PAYER)
            .build();
    when(userRepository.findByPhone(phone)).thenReturn(Optional.of(fraudLockedUser));
    when(otpService.verifyAndConsumeOtp(phone, "123456")).thenReturn(true);

    assertThatThrownBy(() -> authService.verifyOtp(phone, "123456"))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.FORBIDDEN);
    assertThat(fraudLockedUser.getStatus()).isEqualTo(UserStatus.LOCKED);
  }

  @Test
  @DisplayName("login should check credentials and active status")
  void testLogin() {
    LoginRequest req = new LoginRequest("+919876543210", "Password123!");
    when(userRepository.findByPhone(req.phone())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> authService.login(req))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.UNAUTHENTICATED);

    UserEntity lockedUser =
        UserEntity.builder()
            .id(UUID.randomUUID())
            .phone(req.phone())
            .passwordHash("$hash")
            .status(UserStatus.LOCKED)
            .role(UserRole.PAYER)
            .build();
    when(userRepository.findByPhone(req.phone())).thenReturn(Optional.of(lockedUser));
    when(passwordHasher.verifyPassword(req.password(), "$hash")).thenReturn(false);

    // Wrong password
    assertThatThrownBy(() -> authService.login(req))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.UNAUTHENTICATED);

    // Correct password but locked
    when(passwordHasher.verifyPassword(req.password(), "$hash")).thenReturn(true);
    assertThatThrownBy(() -> authService.login(req))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.FORBIDDEN);

    // Active user
    lockedUser.setStatus(UserStatus.ACTIVE);
    when(jwtTokenService.generateAccessToken(any(), anyString())).thenReturn("mock-access");
    when(refreshTokenService.issueRefreshToken(any())).thenReturn("mock-refresh");

    AuthService.LoginResult result = authService.login(req);
    assertThat(result.tokenResponse().accessToken()).isEqualTo("mock-access");
    assertThat(result.refreshToken()).isEqualTo("mock-refresh");
  }

  @Test
  @DisplayName("refresh should validate token and user active state")
  void testRefresh() {
    assertThatThrownBy(() -> authService.refresh(null))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.UNAUTHENTICATED);
    assertThatThrownBy(() -> authService.refresh("   "))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.UNAUTHENTICATED);

    UUID userId = UUID.randomUUID();
    when(refreshTokenService.rotateRefreshToken("raw-token")).thenReturn("new-token");
    when(refreshTokenService.getUserIdFromValidToken("new-token")).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.refresh("raw-token"))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.NOT_FOUND);

    UserEntity user =
        UserEntity.builder()
            .id(userId)
            .phone("+919876543210")
            .role(UserRole.MERCHANT)
            .status(UserStatus.LOCKED)
            .build();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> authService.refresh("raw-token"))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.FORBIDDEN);

    user.setStatus(UserStatus.ACTIVE);
    when(merchantService.findByUserId(userId))
        .thenReturn(
            Optional.of(
                new Merchant(
                    UUID.randomUUID(),
                    userId,
                    "Ravi Store",
                    "r@hey",
                    "GENERAL",
                    com.heypayments.merchant.domain.model.MerchantStatus.ACTIVE,
                    100L,
                    now,
                    now)));
    when(jwtTokenService.generateAccessToken(any(), anyString())).thenReturn("new-access");

    AuthService.LoginResult res = authService.refresh("raw-token");
    assertThat(res.tokenResponse().user().displayName()).isEqualTo("Ravi Store");

    // Logout
    authService.logout("raw-token");
    verify(refreshTokenService).revokeRefreshToken("raw-token");
  }

  @Test
  @DisplayName("setPin should validate user role and current password")
  void testSetPin() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> authService.setPin(userId, "123456", "pwd"))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.NOT_FOUND);

    UserEntity merchant =
        UserEntity.builder().id(userId).role(UserRole.MERCHANT).passwordHash("$hash").build();
    when(userRepository.findById(userId)).thenReturn(Optional.of(merchant));

    // Only payers can set PIN
    assertThatThrownBy(() -> authService.setPin(userId, "123456", "pwd"))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.FORBIDDEN);

    UserEntity payer =
        UserEntity.builder().id(userId).role(UserRole.PAYER).passwordHash("$hash").build();
    when(userRepository.findById(userId)).thenReturn(Optional.of(payer));
    when(passwordHasher.verifyPassword("wrong", "$hash")).thenReturn(false);

    // Incorrect password
    assertThatThrownBy(() -> authService.setPin(userId, "123456", "wrong"))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.UNAUTHENTICATED);

    // Valid
    when(passwordHasher.verifyPassword("correct", "$hash")).thenReturn(true);
    when(passwordHasher.hashPin("123456")).thenReturn("$pinHash");
    authService.setPin(userId, "123456", "correct");
    assertThat(payer.getPinHash()).isEqualTo("$pinHash");
  }

  @Test
  @DisplayName("getProfile should return profile or throw NOT_FOUND")
  void testGetProfile() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> authService.getProfile(userId))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.NOT_FOUND);

    UserEntity user =
        UserEntity.builder()
            .id(userId)
            .phone("+919876543210")
            .email("p@hey.dev")
            .role(UserRole.PAYER)
            .status(UserStatus.ACTIVE)
            .createdAt(now)
            .build();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    UserProfileResponse profile = authService.getProfile(userId);
    assertThat(profile.displayName()).isEqualTo("+919876543210");
  }
}
