package com.heypayments.shared.infra.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.heypayments.BaseIntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class AuthAndDeviceSchemaMigrationTest extends BaseIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void cleanUp() {
    jdbcTemplate.execute("DELETE FROM server_keys");
    jdbcTemplate.execute("DELETE FROM devices");
    jdbcTemplate.execute("DELETE FROM merchants");
    jdbcTemplate.execute("DELETE FROM refresh_tokens");
    jdbcTemplate.execute("DELETE FROM wallets");
    jdbcTemplate.execute("DELETE FROM users");
  }

  @Test
  @DisplayName("V2 migration: users table enforces unique phone and cascading refresh_tokens")
  void shouldVerifyUsersAndRefreshTokensSchema() {
    UUID userId = UUID.randomUUID();
    String phone = "+919876543210";
    Timestamp now = Timestamp.from(Instant.now());

    jdbcTemplate.update(
        "INSERT INTO users (id, phone, email, password_hash, role, status, created_at, updated_at)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        userId,
        phone,
        "payer@example.com",
        "hashed_pwd",
        "PAYER",
        "ACTIVE",
        now,
        now);

    // Duplicate phone must violate unique constraint
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO users (id, phone, email, password_hash, role, status, created_at,"
                        + " updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    UUID.randomUUID(),
                    phone,
                    "other@example.com",
                    "hashed_pwd2",
                    "PAYER",
                    "ACTIVE",
                    now,
                    now))
        .isInstanceOf(DataIntegrityViolationException.class);

    // Refresh token referencing user
    UUID tokenId = UUID.randomUUID();
    UUID familyId = UUID.randomUUID();
    String tokenHash = "hash1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";

    jdbcTemplate.update(
        "INSERT INTO refresh_tokens (id, user_id, token_hash, family_id, expires_at, created_at)"
            + " VALUES (?, ?, ?, ?, ?, ?)",
        tokenId,
        userId,
        tokenHash,
        familyId,
        Timestamp.from(Instant.now().plusSeconds(3600)),
        now);

    // Duplicate token_hash must fail
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO refresh_tokens (id, user_id, token_hash, family_id, expires_at,"
                        + " created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    UUID.randomUUID(),
                    userId,
                    tokenHash,
                    familyId,
                    Timestamp.from(Instant.now().plusSeconds(3600)),
                    now))
        .isInstanceOf(DataIntegrityViolationException.class);

    // Cascade delete on user
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    Integer tokenCount =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM refresh_tokens WHERE id = ?", Integer.class, tokenId);
    assertThat(tokenCount).isZero();
  }

  @Test
  @DisplayName(
      "V3 migration: devices enforces single active device per user per kind, merchants enforces"
          + " unique handle and user, server_keys enforces kid PK")
  void shouldVerifyDevicesMerchantsAndServerKeysSchema() {
    UUID userId = UUID.randomUUID();
    Timestamp now = Timestamp.from(Instant.now());

    jdbcTemplate.update(
        "INSERT INTO users (id, phone, email, password_hash, role, status, created_at, updated_at)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        userId,
        "+919876543211",
        "merchant@example.com",
        "hashed_pwd",
        "MERCHANT",
        "ACTIVE",
        now,
        now);

    // First active device for PAYER kind
    UUID dev1 = UUID.randomUUID();
    byte[] pubKey = new byte[65];
    pubKey[0] = 0x04;
    jdbcTemplate.update(
        "INSERT INTO devices (id, user_id, kind, public_key, label, user_agent, status,"
            + " last_counter, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        dev1,
        userId,
        "PAYER",
        pubKey,
        "Phone A",
        "Chrome/Android",
        "ACTIVE",
        0L,
        now,
        now);

    // Second active device for SAME kind and user must fail unique partial index
    // ux_device_active_user
    UUID dev2 = UUID.randomUUID();
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO devices (id, user_id, kind, public_key, label, user_agent, status,"
                        + " last_counter, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?,"
                        + " ?, ?)",
                    dev2,
                    userId,
                    "PAYER",
                    pubKey,
                    "Phone B",
                    "Chrome/Android",
                    "ACTIVE",
                    0L,
                    now,
                    now))
        .isInstanceOf(DataIntegrityViolationException.class);

    // Merchant table insert
    UUID merchantId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO merchants (id, user_id, display_name, handle, category, status, pending_cap,"
            + " created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        merchantId,
        userId,
        "Ravi General Store",
        "ravi.store@hey",
        "GROCERY",
        "ACTIVE",
        1000000L,
        now,
        now);

    // Duplicate merchant handle must fail
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO merchants (id, user_id, display_name, handle, category, status,"
                        + " pending_cap, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?,"
                        + " ?)",
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Another Store",
                    "ravi.store@hey",
                    "GROCERY",
                    "ACTIVE",
                    1000000L,
                    now,
                    now))
        .isInstanceOf(DataIntegrityViolationException.class);

    // Server keys table insert
    String kid = "srv-2026-11";
    jdbcTemplate.update(
        "INSERT INTO server_keys (kid, public_key, private_key_enc, status, not_before, not_after,"
            + " created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
        kid,
        pubKey,
        new byte[] {0x10, 0x20},
        "ACTIVE",
        now,
        Timestamp.from(Instant.now().plusSeconds(86400 * 90)),
        now);

    // Duplicate kid must fail PK
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO server_keys (kid, public_key, private_key_enc, status, not_before,"
                        + " not_after, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    kid,
                    pubKey,
                    new byte[] {0x10, 0x20},
                    "ACTIVE",
                    now,
                    Timestamp.from(Instant.now().plusSeconds(86400 * 90)),
                    now))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
