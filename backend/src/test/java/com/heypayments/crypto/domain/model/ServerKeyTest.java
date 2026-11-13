package com.heypayments.crypto.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ServerKeyTest {

  @Test
  @DisplayName(
      "ServerKey isActive and canVerify checks should evaluate all status and validity window branches")
  void testServerKeyBranches() {
    Instant now = Instant.now();
    Instant notBefore = now.minus(1, ChronoUnit.DAYS);
    Instant notAfter = now.plus(89, ChronoUnit.DAYS);

    ServerKey activeKey =
        new ServerKey(
            "kid-1",
            new byte[] {1},
            new byte[] {2},
            ServerKeyStatus.ACTIVE,
            notBefore,
            notAfter,
            notBefore);

    assertThat(activeKey.purpose()).isEqualTo(ServerKeyPurpose.PROTOCOL);
    assertThat(activeKey.isActive(now)).isTrue();
    assertThat(activeKey.canVerify(now)).isTrue();

    // Before notBefore
    assertThat(activeKey.isActive(notBefore.minus(1, ChronoUnit.SECONDS))).isFalse();
    assertThat(activeKey.canVerify(notBefore.minus(1, ChronoUnit.SECONDS))).isFalse();

    // After notAfter
    assertThat(activeKey.isActive(notAfter.plus(1, ChronoUnit.SECONDS))).isFalse();

    // Retiring key
    ServerKey retiringKey =
        new ServerKey(
            "kid-2",
            ServerKeyPurpose.JWT,
            new byte[] {1},
            new byte[] {2},
            ServerKeyStatus.RETIRING,
            notBefore,
            notAfter,
            notBefore);
    assertThat(retiringKey.isActive(now)).isFalse();
    assertThat(retiringKey.canVerify(now)).isTrue();

    // Revoked key
    ServerKey revokedKey =
        new ServerKey(
            "kid-3",
            ServerKeyPurpose.PROTOCOL,
            new byte[] {1},
            new byte[] {2},
            ServerKeyStatus.REVOKED,
            notBefore,
            notAfter,
            notBefore);
    assertThat(revokedKey.isActive(now)).isFalse();
    assertThat(revokedKey.canVerify(now)).isFalse();
  }
}
