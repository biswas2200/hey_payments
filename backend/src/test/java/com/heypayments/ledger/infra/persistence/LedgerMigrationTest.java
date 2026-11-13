package com.heypayments.ledger.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.heypayments.BaseIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class LedgerMigrationTest extends BaseIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TransactionTemplate transactionTemplate;

  @Test
  @DisplayName("V5 & V6 migrations: tables must exist and system accounts must be seeded")
  void shouldApplyMigrationsAndSeedSystemAccounts() {
    // Assert tables exist in information_schema
    Integer accountsTableCount =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM information_schema.tables WHERE table_name = 'ledger_accounts'",
            Integer.class);
    assertThat(accountsTableCount).isEqualTo(1);

    Integer entriesTableCount =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM information_schema.tables WHERE table_name = 'journal_entries'",
            Integer.class);
    assertThat(entriesTableCount).isEqualTo(1);

    Integer linesTableCount =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM information_schema.tables WHERE table_name = 'journal_lines'",
            Integer.class);
    assertThat(linesTableCount).isEqualTo(1);

    Integer walletsTableCount =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM information_schema.tables WHERE table_name = 'wallets'",
            Integer.class);
    assertThat(walletsTableCount).isEqualTo(1);

    // Assert system accounts exist
    Long bankSimCount =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM ledger_accounts WHERE code = 'SYS:BANK_SIM_CLEARING' AND type = 'BANK_SIM_CLEARING'",
            Long.class);
    assertThat(bankSimCount).isEqualTo(1L);

    Long platformFeesCount =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM ledger_accounts WHERE code = 'SYS:PLATFORM_FEES' AND type = 'PLATFORM_FEES'",
            Long.class);
    assertThat(platformFeesCount).isEqualTo(1L);

    Long riskReserveCount =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM ledger_accounts WHERE code = 'SYS:RISK_RESERVE' AND type = 'RISK_RESERVE'",
            Long.class);
    assertThat(riskReserveCount).isEqualTo(1L);
  }

  @Test
  @DisplayName("V5 migration: deferred trigger trg_journal_balanced must reject unbalanced lines")
  void shouldEnforceBalancedJournalEntryTrigger() {
    UUID entryId = UUID.randomUUID();
    UUID accountId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM ledger_accounts WHERE code = 'SYS:BANK_SIM_CLEARING'", UUID.class);
    assertThat(accountId).isNotNull();

    // Trying to commit an unbalanced transaction with SUM(amount) != 0 must fail on commit
    assertThatThrownBy(
            () ->
                transactionTemplate.execute(
                    status -> {
                      jdbcTemplate.update(
                          "INSERT INTO journal_entries (id, kind, reference, memo) VALUES (?, 'TEST', ?, 'Unbalanced test')",
                          entryId,
                          "ref-unbalanced-" + UUID.randomUUID());
                      jdbcTemplate.update(
                          "INSERT INTO journal_lines (id, entry_id, account_id, amount) VALUES (?, ?, ?, 5000)",
                          UUID.randomUUID(),
                          entryId,
                          accountId);
                      jdbcTemplate.update(
                          "INSERT INTO journal_lines (id, entry_id, account_id, amount) VALUES (?, ?, ?, -4000)",
                          UUID.randomUUID(),
                          entryId,
                          accountId);
                      return null;
                    }))
        .isNotNull();
  }
}
