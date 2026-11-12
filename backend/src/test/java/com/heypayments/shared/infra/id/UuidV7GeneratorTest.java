package com.heypayments.shared.infra.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UuidV7GeneratorTest {

  @Test
  @DisplayName("should generate a valid UUIDv7")
  void shouldGenerateValidUuidV7() {
    UUID uuid = UuidV7Generator.generate();

    assertThat(uuid).isNotNull();
    assertThat(uuid.version()).isEqualTo(7);
    assertThat(uuid.variant()).isEqualTo(2);
  }

  @Test
  @DisplayName("should extract approximate timestamp from UUIDv7")
  void shouldExtractTimestamp() {
    long before = System.currentTimeMillis();
    UUID uuid = UuidV7Generator.generate();
    long after = System.currentTimeMillis();

    long extractedMillis = UuidV7Generator.extractTimestamp(uuid);

    assertThat(extractedMillis).isBetween(before, after);
  }

  @Test
  @DisplayName("should reject null UUID on timestamp extraction")
  void shouldRejectNullUuid() {
    assertThatThrownBy(() -> UuidV7Generator.extractTimestamp(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be null");
  }

  @Test
  @DisplayName("should generate unique IDs sequentially")
  void shouldGenerateUniqueIds() {
    Set<UUID> set = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
      UUID id = UuidV7Generator.generate();
      assertThat(set.add(id)).as("UUIDv7 should be unique: " + id).isTrue();
    }
  }
}
