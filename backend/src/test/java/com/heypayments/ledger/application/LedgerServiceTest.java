package com.heypayments.ledger.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.heypayments.BaseIntegrationTest;
import com.heypayments.ledger.domain.model.AccountType;
import com.heypayments.ledger.domain.model.EntryKind;
import com.heypayments.ledger.domain.model.JournalEntry;
import com.heypayments.ledger.domain.model.LedgerAccount;
import com.heypayments.shared.domain.model.Money;
import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class LedgerServiceTest extends BaseIntegrationTest {

  @Autowired private LedgerService ledgerService;
  @Autowired private JdbcTemplate jdbcTemplate;

  private LedgerAccount userMainAccount;
  private LedgerAccount bankSimAccount;

  @BeforeEach
  void setUp() {
    UUID userId = UUID.randomUUID();
    userMainAccount =
        ledgerService.getOrCreateAccount("USER_MAIN:" + userId, AccountType.USER_MAIN, userId);
    bankSimAccount = ledgerService.getAccountByCode("SYS:BANK_SIM_CLEARING");
  }

  @Test
  @DisplayName("should post balanced journal entry and update account balances")
  void shouldPostBalancedEntrySuccessfully() {
    String ref = "fund-ref-" + UUID.randomUUID();
    PostJournalEntryCommand command =
        new PostJournalEntryCommand(
            EntryKind.FUNDING,
            ref,
            "Simulated UPI funding",
            List.of(
                new PostJournalLineCommand(bankSimAccount.code(), -5000L),
                new PostJournalLineCommand(userMainAccount.code(), 5000L)));

    JournalEntry entry = ledgerService.post(command);

    assertThat(entry).isNotNull();
    assertThat(entry.reference()).isEqualTo(ref);
    assertThat(entry.lines()).hasSize(2);

    Money userBalance = ledgerService.getBalance(userMainAccount.code());
    assertThat(userBalance.paise()).isEqualTo(5000L);

    // Verify audit log record exists for this posting
    Long auditCount =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM audit_log WHERE entity = 'JOURNAL_ENTRY' AND entity_id = ?",
            Long.class,
            entry.id());
    assertThat(auditCount).isGreaterThanOrEqualTo(1L);
  }

  @Test
  @DisplayName("should reject unbalanced entry where sum of amounts is non-zero (Σ!=0)")
  void shouldRejectUnbalancedEntry() {
    String ref = "unbalanced-" + UUID.randomUUID();
    PostJournalEntryCommand command =
        new PostJournalEntryCommand(
            EntryKind.FUNDING,
            ref,
            "Unbalanced",
            List.of(
                new PostJournalLineCommand(bankSimAccount.code(), -5000L),
                new PostJournalLineCommand(userMainAccount.code(), 3000L)));

    assertThatThrownBy(() -> ledgerService.post(command))
        .isInstanceOf(DomainException.class)
        .satisfies(
            ex -> {
              DomainException de = (DomainException) ex;
              assertThat(de.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
              assertThat(de.getMessage()).contains("unbalanced");
            });
  }

  @Test
  @DisplayName("should reject entry with fewer than 2 lines")
  void shouldRejectEntryWithLessThanTwoLines() {
    String ref = "single-line-" + UUID.randomUUID();
    PostJournalEntryCommand command =
        new PostJournalEntryCommand(
            EntryKind.ADJUSTMENT,
            ref,
            "Single line",
            List.of(new PostJournalLineCommand(userMainAccount.code(), 0L)));

    assertThatThrownBy(() -> ledgerService.post(command))
        .isInstanceOf(DomainException.class)
        .satisfies(
            ex -> {
              DomainException de = (DomainException) ex;
              assertThat(de.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
            });
  }

  @Test
  @DisplayName("should reject posting when USER_MAIN account would become negative")
  void shouldRejectNegativeBalanceForUserMain() {
    String ref = "overdraft-" + UUID.randomUUID();
    PostJournalEntryCommand command =
        new PostJournalEntryCommand(
            EntryKind.LOAD_OFFLINE,
            ref,
            "Load overdraft",
            List.of(
                new PostJournalLineCommand(userMainAccount.code(), -1000L),
                new PostJournalLineCommand(bankSimAccount.code(), 1000L)));

    assertThatThrownBy(() -> ledgerService.post(command))
        .isInstanceOf(DomainException.class)
        .satisfies(
            ex -> {
              DomainException de = (DomainException) ex;
              assertThat(de.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
            });
  }

  @Test
  @DisplayName("should be idempotent when posting entry with existing reference")
  void shouldBeIdempotentOnDuplicateReference() {
    String ref = "idempotent-ref-" + UUID.randomUUID();
    PostJournalEntryCommand command =
        new PostJournalEntryCommand(
            EntryKind.FUNDING,
            ref,
            "Funding repeat",
            List.of(
                new PostJournalLineCommand(bankSimAccount.code(), -10000L),
                new PostJournalLineCommand(userMainAccount.code(), 10000L)));

    JournalEntry first = ledgerService.post(command);
    JournalEntry second = ledgerService.post(command);

    assertThat(second.id()).isEqualTo(first.id());
    assertThat(second.reference()).isEqualTo(first.reference());

    // Balance must be credited only once
    Money userBalance = ledgerService.getBalance(userMainAccount.code());
    assertThat(userBalance.paise()).isEqualTo(10000L);
  }
}
