package com.heypayments.shared.infra.db;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.heypayments.BaseIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class AuditLogTriggerTest extends BaseIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName(
      "audit_log table should allow INSERT but strictly forbid UPDATE, DELETE, and TRUNCATE")
  void shouldEnforceAppendOnlyOnAuditLog() {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO audit_log (id, action, entity, entity_id) VALUES (?, ?, ?, ?)",
        id,
        "TEST_ACTION",
        "TEST_ENTITY",
        "123");

    assertThatThrownBy(
            () -> jdbcTemplate.update("UPDATE audit_log SET action = 'MODIFIED' WHERE id = ?", id))
        .hasMessageContaining("audit_log is append-only");

    assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM audit_log WHERE id = ?", id))
        .hasMessageContaining("audit_log is append-only");

    assertThatThrownBy(() -> jdbcTemplate.execute("TRUNCATE TABLE audit_log"))
        .hasMessageContaining("audit_log is append-only");
  }
}
