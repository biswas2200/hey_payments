package com.heypayments.shared.infra.id;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Generator for time-ordered UUIDv7 identifiers according to RFC 9562. Used for primary keys across
 * all domain entities and records.
 */
public final class UuidV7Generator {

  private static final SecureRandom RANDOM = new SecureRandom();

  private UuidV7Generator() {
    // Utility class
  }

  public static UUID generate() {
    long timestamp = System.currentTimeMillis();

    long randA = RANDOM.nextLong() & 0x0FFFL; // 12 bits
    long randB = RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL; // 62 bits

    // 48 bits timestamp | 4 bits version (0x7) | 12 bits random
    long mostSigBits = (timestamp << 16) | (0x7000L) | randA;

    // 2 bits variant (0b10 = 0x8) | 62 bits random
    long leastSigBits = 0x8000000000000000L | randB;

    return new UUID(mostSigBits, leastSigBits);
  }

  public static long extractTimestamp(UUID uuid) {
    if (uuid == null) {
      throw new IllegalArgumentException("UUID cannot be null");
    }
    return uuid.getMostSignificantBits() >>> 16;
  }
}
