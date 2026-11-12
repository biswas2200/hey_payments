package com.heypayments.auth.application;

import com.heypayments.auth.domain.model.User;
import com.heypayments.auth.domain.model.UserRole;
import com.heypayments.auth.domain.model.UserStatus;
import com.heypayments.auth.domain.service.PasswordHasher;
import com.heypayments.auth.infra.persistence.UserEntity;
import com.heypayments.auth.infra.persistence.UserRepository;
import com.heypayments.auth.web.dto.LoginRequest;
import com.heypayments.auth.web.dto.RegisterRequest;
import com.heypayments.auth.web.dto.TokenResponse;
import com.heypayments.auth.web.dto.UserProfileResponse;
import com.heypayments.auth.web.dto.UserSummaryDto;
import com.heypayments.merchant.application.MerchantService;
import com.heypayments.merchant.domain.model.Merchant;
import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import com.heypayments.shared.infra.id.UuidV7Generator;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final MerchantService merchantService;
  private final PasswordHasher passwordHasher;
  private final JwtTokenService jwtTokenService;
  private final RefreshTokenService refreshTokenService;
  private final OtpService otpService;
  private final Clock clock;

  public record LoginResult(TokenResponse tokenResponse, String refreshToken) {}

  @Transactional
  public UUID register(RegisterRequest req) {
    if (userRepository.existsByPhone(req.phone())) {
      throw new DomainException(ErrorCode.CONFLICT, "Phone number is already registered");
    }

    Instant now = clock.instant();
    UUID userId = UuidV7Generator.generate();
    UserRole role = UserRole.valueOf(req.role());
    String hashedPassword = passwordHasher.hashPassword(req.password());

    UserEntity user =
        UserEntity.builder()
            .id(userId)
            .phone(req.phone())
            .email(req.email())
            .passwordHash(hashedPassword)
            .role(role)
            .status(UserStatus.LOCKED) // Locked until phone is verified via OTP
            .createdAt(now)
            .updatedAt(now)
            .build();
    userRepository.save(user);

    if (role == UserRole.MERCHANT) {
      String displayName = req.displayName() != null ? req.displayName() : "Merchant Store";
      String baseHandle =
          displayName.toLowerCase().replaceAll("[^a-z0-9]", ".").replaceAll("\\.+", ".");
      String handle = baseHandle + "@hey";
      if (merchantService.existsByHandle(handle)) {
        handle = baseHandle + "." + userId.toString().substring(0, 4) + "@hey";
      }

      merchantService.createMerchant(userId, displayName, handle, "GENERAL", 1_000_000L);
    }

    otpService.generateAndSendOtp(req.phone(), req.email());
    log.info("Registered user id={} role={} status=LOCKED, OTP dispatched", userId, role);
    return userId;
  }

  @Transactional
  public void verifyOtp(String phone, String otp) {
    boolean valid = otpService.verifyAndConsumeOtp(phone, otp);
    if (!valid) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Invalid or expired OTP");
    }

    UserEntity user =
        userRepository
            .findByPhone(phone)
            .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User not found"));

    user.setStatus(UserStatus.ACTIVE);
    user.setUpdatedAt(clock.instant());
    userRepository.save(user);
    log.info("User id={} phone={} successfully verified and activated", user.getId(), phone);
  }

  @Transactional
  public LoginResult login(LoginRequest req) {
    UserEntity user =
        userRepository
            .findByPhone(req.phone())
            .orElseThrow(
                () -> new DomainException(ErrorCode.UNAUTHENTICATED, "Invalid phone or password"));

    if (!passwordHasher.verifyPassword(req.password(), user.getPasswordHash())) {
      throw new DomainException(ErrorCode.UNAUTHENTICATED, "Invalid phone or password");
    }

    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new DomainException(
          ErrorCode.FORBIDDEN,
          "Account is not active or is locked. Please verify OTP or contact support.");
    }

    String displayName = resolveDisplayName(user);
    User domainUser = toDomain(user);

    String accessToken = jwtTokenService.generateAccessToken(domainUser, displayName);
    String refreshToken = refreshTokenService.issueRefreshToken(user.getId());

    TokenResponse response =
        new TokenResponse(
            accessToken, 900, new UserSummaryDto(user.getId(), user.getRole().name(), displayName));

    return new LoginResult(response, refreshToken);
  }

  @Transactional(noRollbackFor = AuthTokenException.class)
  public LoginResult refresh(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      throw new DomainException(ErrorCode.UNAUTHENTICATED, "Missing refresh token");
    }

    String rotatedRefreshToken = refreshTokenService.rotateRefreshToken(rawRefreshToken);
    UUID userId = refreshTokenService.getUserIdFromValidToken(rotatedRefreshToken);

    UserEntity user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User not found"));

    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new DomainException(ErrorCode.FORBIDDEN, "Account is not active");
    }

    String displayName = resolveDisplayName(user);
    String accessToken = jwtTokenService.generateAccessToken(toDomain(user), displayName);

    TokenResponse response =
        new TokenResponse(
            accessToken, 900, new UserSummaryDto(user.getId(), user.getRole().name(), displayName));

    return new LoginResult(response, rotatedRefreshToken);
  }

  @Transactional
  public void logout(String rawRefreshToken) {
    refreshTokenService.revokeRefreshToken(rawRefreshToken);
  }

  @Transactional
  public void setPin(UUID userId, String pin, String password) {
    UserEntity user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User not found"));

    if (user.getRole() != UserRole.PAYER) {
      throw new DomainException(ErrorCode.FORBIDDEN, "Only payers can configure payment PIN");
    }

    if (!passwordHasher.verifyPassword(password, user.getPasswordHash())) {
      throw new DomainException(ErrorCode.UNAUTHENTICATED, "Incorrect password");
    }

    String hashedPin = passwordHasher.hashPin(pin);
    user.setPinHash(hashedPin);
    user.setUpdatedAt(clock.instant());
    userRepository.save(user);
    log.info("Payment PIN successfully configured for user id={}", userId);
  }

  @Transactional(readOnly = true)
  public UserProfileResponse getProfile(UUID userId) {
    UserEntity user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User not found"));

    String displayName = resolveDisplayName(user);
    return new UserProfileResponse(
        user.getId(),
        user.getPhone(),
        user.getEmail(),
        user.getRole().name(),
        user.getStatus().name(),
        displayName,
        user.getCreatedAt());
  }

  private String resolveDisplayName(UserEntity user) {
    if (user.getRole() == UserRole.MERCHANT) {
      return merchantService
          .findByUserId(user.getId())
          .map(Merchant::displayName)
          .orElse(user.getPhone());
    }
    return user.getPhone();
  }

  private User toDomain(UserEntity entity) {
    return new User(
        entity.getId(),
        entity.getPhone(),
        entity.getEmail(),
        entity.getPasswordHash(),
        entity.getRole(),
        entity.getStatus(),
        entity.getPinHash(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
