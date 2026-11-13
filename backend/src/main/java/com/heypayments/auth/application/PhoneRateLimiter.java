package com.heypayments.auth.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * In-memory per-phone rate limiter for login and OTP verification.
 * Enforces 5 requests per minute per phone number across all IP addresses.
 */
@Slf4j
@Component
public class PhoneRateLimiter {

  private final Cache<String, Bucket> phoneBuckets =
      Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(15)).maximumSize(10_000).build();

  public void consumeOrThrow(String phone) {
    if (phone == null || phone.isBlank()) {
      return;
    }

    Bucket bucket = phoneBuckets.get(phone, k -> createPhoneBucket());
    if (!bucket.tryConsume(1)) {
      log.warn("Per-phone rate limit exceeded for phone=[PROTECTED]");
      throw new DomainException(
          ErrorCode.RATE_LIMITED, "Too many requests for this phone number. Please try again later.");
    }
  }

  private Bucket createPhoneBucket() {
    Refill refill = Refill.greedy(5, Duration.ofMinutes(1));
    Bandwidth limit = Bandwidth.classic(5, refill);
    return Bucket.builder().addLimit(limit).build();
  }

  public void reset() {
    phoneBuckets.invalidateAll();
  }
}
