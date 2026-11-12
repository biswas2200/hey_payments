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
}
