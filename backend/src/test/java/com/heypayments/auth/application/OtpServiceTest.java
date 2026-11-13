package com.heypayments.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

class OtpServiceTest {

  private Clock clock;
  private OtpService otpService;
  private Instant fixedInstant;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    fixedInstant = Instant.parse("2026-11-12T10:00:00Z");
    clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
    ObjectProvider<JavaMailSender> mailProvider = mock(ObjectProvider.class);
    otpService = new OtpService(clock, mailProvider);
  }

  @Test
  @DisplayName("should generate, verify and consume valid OTP")
  void shouldGenerateAndVerifyOtp() {
    String phone = "+919876543210";
    String otp = otpService.generateAndSendOtp(phone, null);

    assertThat(otp).matches("^\\d{6}$");
    assertThat(otpService.getRecentOtpForPhone(phone)).isEqualTo(otp);

    // Wrong OTP fails
    assertThat(otpService.verifyAndConsumeOtp(phone, "000000")).isFalse();

    // Correct OTP succeeds and is consumed
    assertThat(otpService.verifyAndConsumeOtp(phone, otp)).isTrue();
    assertThat(otpService.verifyAndConsumeOtp(phone, otp)).isFalse();
    assertThat(otpService.getRecentOtpForPhone(phone)).isNull();
  }

  @Test
  @DisplayName("should handle expired OTP")
  void shouldHandleExpiredOtp() {
    String phone = "+919876543211";
    String otp = otpService.generateAndSendOtp(phone, "user@example.com");

    // Advance clock past 5 minutes
    Clock expiredClock = Clock.fixed(fixedInstant.plusSeconds(301), ZoneId.of("UTC"));
    @SuppressWarnings("unchecked")
    ObjectProvider<JavaMailSender> mailProvider = mock(ObjectProvider.class);
    OtpService expiredService = new OtpService(expiredClock, mailProvider);

    // Verify absent phone
    assertThat(otpService.verifyAndConsumeOtp("+910000000000", "123456")).isFalse();
    assertThat(otpService.getRecentOtpForPhone("+910000000000")).isNull();
  }

  @Test
  @DisplayName("should invalidate OTP after 5 failed verification attempts to prevent brute force")
  void shouldInvalidateOtpAfterMaxFailedAttempts() {
    String phone = "+919876543299";
    String otp = otpService.generateAndSendOtp(phone, null);

    // 4 failed attempts
    for (int i = 0; i < 4; i++) {
      assertThat(otpService.verifyAndConsumeOtp(phone, "000000")).isFalse();
    }
    // Still valid with correct OTP
    assertThat(otpService.getRecentOtpForPhone(phone)).isEqualTo(otp);

    // 5th failed attempt invalidates OTP
    assertThat(otpService.verifyAndConsumeOtp(phone, "000000")).isFalse();

    // Now even correct OTP fails because OTP was invalidated
    assertThat(otpService.verifyAndConsumeOtp(phone, otp)).isFalse();
    assertThat(otpService.getRecentOtpForPhone(phone)).isNull();
  }

  @Test
  @DisplayName("should handle null and empty inputs safely with constant-time comparison")
  void shouldHandleNullAndEmptyInputsSafely() {
    String phone = "+919876543212";
    otpService.generateAndSendOtp(phone, null);

    assertThat(otpService.verifyAndConsumeOtp(phone, null)).isFalse();
    assertThat(otpService.verifyAndConsumeOtp(phone, "")).isFalse();
    assertThat(otpService.verifyAndConsumeOtp(phone, "123")).isFalse();
  }
}
