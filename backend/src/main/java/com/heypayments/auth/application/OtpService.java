package com.heypayments.auth.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

  private static final long OTP_VALIDITY_SECONDS = 300; // 5 minutes
  private static final int MAX_FAILED_ATTEMPTS = 5;

  private final Clock clock;
  private final org.springframework.beans.factory.ObjectProvider<JavaMailSender> mailSenderProvider;
  private final SecureRandom secureRandom = new SecureRandom();

  private record OtpEntry(String otp, Instant expiresAt, int failedAttempts) {}

  private final Cache<String, OtpEntry> otpStore =
      Caffeine.newBuilder()
          .expireAfterWrite(Duration.ofMinutes(10))
          .maximumSize(10_000)
          .build();

  public String generateAndSendOtp(String phone, String email) {
    int code = secureRandom.nextInt(1_000_000);
    String otp = String.format("%06d", code);
    Instant expiresAt = clock.instant().plusSeconds(OTP_VALIDITY_SECONDS);

    otpStore.put(phone, new OtpEntry(otp, expiresAt, 0));
    log.info("Generated OTP for phone {} (expires in 5m)", phone);

    // Send email via Mailpit (SMTP port 1025) if mailSender is available
    if (email != null && !email.isBlank()) {
      mailSenderProvider.ifAvailable(
          mailSender -> {
            try {
              SimpleMailMessage message = new SimpleMailMessage();
              message.setFrom("auth@heypayments.dev");
              message.setTo(email);
              message.setSubject("Hey Payments Verification OTP");
              message.setText(
                  "Your Hey Payments verification code is: " + otp + "\nValid for 5 minutes.");
              mailSender.send(message);
              log.info("Sent verification email with OTP to {}", email);
            } catch (Exception e) {
              log.warn("Could not dispatch email to Mailpit: {}", e.getMessage());
            }
          });
    }

    return otp;
  }

  public boolean verifyAndConsumeOtp(String phone, String inputOtp) {
    OtpEntry entry = otpStore.getIfPresent(phone);
    if (entry == null) {
      return false;
    }
    if (clock.instant().isAfter(entry.expiresAt())) {
      otpStore.invalidate(phone);
      return false;
    }
    if (entry.otp().equals(inputOtp)) {
      otpStore.invalidate(phone);
      return true;
    }

    int nextFailedAttempts = entry.failedAttempts() + 1;
    if (nextFailedAttempts >= MAX_FAILED_ATTEMPTS) {
      log.warn(
          "OTP for phone {} invalidated after reaching max failed attempts ({})",
          phone,
          MAX_FAILED_ATTEMPTS);
      otpStore.invalidate(phone);
    } else {
      otpStore.put(
          phone, new OtpEntry(entry.otp(), entry.expiresAt(), nextFailedAttempts));
    }
    return false;
  }

  public String getRecentOtpForPhone(String phone) {
    OtpEntry entry = otpStore.getIfPresent(phone);
    if (entry != null && !clock.instant().isAfter(entry.expiresAt())) {
      return entry.otp();
    }
    return null;
  }
}
