package com.heypayments.ledger.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.heypayments.BaseIntegrationTest;
import com.heypayments.ledger.domain.model.AccountType;
import com.heypayments.ledger.domain.model.EntryKind;
import com.heypayments.ledger.domain.model.LedgerAccount;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class LedgerConcurrencyIntegrationTest extends BaseIntegrationTest {

  @Autowired private LedgerService ledgerService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("100 concurrent requests against ledger preserve conservation of money (Σ=0)")
  void oneHundredConcurrentRequestsConserveMoney() throws Exception {
    int concurrentRequests = 100;
    ExecutorService executor = Executors.newFixedThreadPool(16);
    CountDownLatch startLatch = new CountDownLatch(1);

    List<LedgerAccount> userAccounts = new ArrayList<>();
    for (int i = 0; i < concurrentRequests; i++) {
      UUID userId = UUID.randomUUID();
      userAccounts.add(
          ledgerService.getOrCreateAccount("USER_MAIN:" + userId, AccountType.USER_MAIN, userId));
    }

    String clearingCode = "SYS:BANK_SIM_CLEARING";
    List<Callable<Boolean>> tasks = new ArrayList<>();

    for (int i = 0; i < concurrentRequests; i++) {
      final int index = i;
      final LedgerAccount targetAccount = userAccounts.get(index);
      final String ref = "concurrent-fund-" + UUID.randomUUID();

      tasks.add(
          () -> {
            startLatch.await(10, TimeUnit.SECONDS);
            try {
              ledgerService.post(
                  new PostJournalEntryCommand(
                      EntryKind.FUNDING,
                      ref,
                      "Concurrent test funding " + index,
                      List.of(
                          new PostJournalLineCommand(clearingCode, -1000L),
                          new PostJournalLineCommand(targetAccount.code(), 1000L))));
              return true;
            } catch (Exception e) {
              return false;
            }
          });
    }

    // Release all threads at once
    List<Future<Boolean>> futures = new ArrayList<>();
    for (var task : tasks) {
      futures.add(executor.submit(task));
    }
    startLatch.countDown();

    int successCount = 0;
    for (Future<Boolean> future : futures) {
      if (future.get(30, TimeUnit.SECONDS)) {
        successCount++;
      }
    }
    executor.shutdown();

    assertThat(successCount).isGreaterThan(0);

    // Invariant 1: Global sum of all account balances in the database is strictly 0
    Long globalSum =
        jdbcTemplate.queryForObject("SELECT COALESCE(SUM(balance), 0) FROM ledger_accounts", Long.class);
    assertThat(globalSum).isEqualTo(0L);

    // Invariant 2: Balance on each account strictly matches the sum of its journal lines
    List<Long> discrepancies =
        jdbcTemplate.query(
            """
            SELECT la.balance - COALESCE(SUM(jl.amount), 0) AS diff
            FROM ledger_accounts la
            LEFT JOIN journal_lines jl ON jl.account_id = la.id
            GROUP BY la.id, la.balance
            HAVING la.balance <> COALESCE(SUM(jl.amount), 0)
            """,
            (rs, rowNum) -> rs.getLong("diff"));

    assertThat(discrepancies)
        .as("Every account balance must exactly match the sum of its journal lines")
        .isEmpty();

    // Invariant 3: Every journal entry has at least 2 lines and sum(amount) == 0
    List<Long> unbalancedEntries =
        jdbcTemplate.query(
            """
            SELECT entry_id
            FROM journal_lines
            GROUP BY entry_id
            HAVING COUNT(*) < 2 OR SUM(amount) <> 0
            """,
            (rs, rowNum) -> rs.getLong("entry_id"));

    assertThat(unbalancedEntries).isEmpty();
  }
}
