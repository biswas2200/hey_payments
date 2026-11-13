package com.heypayments.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.heypayments.ledger.application.PostJournalEntryCommand;
import com.heypayments.ledger.application.PostJournalLineCommand;
import com.heypayments.ledger.domain.model.AccountType;
import com.heypayments.ledger.domain.model.EntryKind;
import com.heypayments.ledger.infra.persistence.LedgerAccountEntity;
import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

class LedgerInvariantsPropertyTest {

  @Property
  void balancedEntriesConserveMoney(@ForAll("balancedLines") List<Long> amounts) {
    List<PostJournalLineCommand> lines = new ArrayList<>();
    for (int i = 0; i < amounts.size(); i++) {
      lines.add(new PostJournalLineCommand("ACC:" + i, amounts.get(i)));
    }

    PostJournalEntryCommand command =
        new PostJournalEntryCommand(EntryKind.ADJUSTMENT, "ref-test", "test memo", lines);

    // Production command validation must succeed without exception
    command.validate();

    assertThat(command.lines()).hasSizeGreaterThanOrEqualTo(2);
    long lineSum = command.lines().stream().mapToLong(PostJournalLineCommand::amount).sum();
    assertThat(lineSum).isEqualTo(0L);
  }

  @Property
  void unbalancedEntriesMustBeRejected(@ForAll("unbalancedLines") List<Long> amounts) {
    List<PostJournalLineCommand> lines = new ArrayList<>();
    for (int i = 0; i < amounts.size(); i++) {
      lines.add(new PostJournalLineCommand("ACC:" + i, amounts.get(i)));
    }

    PostJournalEntryCommand command =
        new PostJournalEntryCommand(EntryKind.ADJUSTMENT, "ref-test", "test memo", lines);

    // Production command validation must throw VALIDATION_FAILED on unbalanced entries
    assertThatThrownBy(command::validate)
        .isInstanceOf(DomainException.class)
        .matches(ex -> ((DomainException) ex).getErrorCode() == ErrorCode.VALIDATION_FAILED);
  }

  @Property
  void userMainAccountNeverBecomesNegative(
      @ForAll @IntRange(min = 0, max = 1_000_000) int initialBalance,
      @ForAll("randomDeductions") List<Long> deductions) {

    LedgerAccountEntity account =
        LedgerAccountEntity.builder()
            .id(UUID.randomUUID())
            .code("USER_MAIN:test")
            .type(AccountType.USER_MAIN)
            .currency("INR")
            .balance((long) initialBalance)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    for (long deduction : deductions) {
      if (deduction > account.getBalance()) {
        // Production entity applyDelta must reject deduction that would make USER_MAIN balance
        // negative
        assertThatThrownBy(() -> account.applyDelta(-deduction))
            .isInstanceOf(DomainException.class)
            .satisfies(
                ex -> {
                  DomainException de = (DomainException) ex;
                  assertThat(de.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
                });
      } else {
        account.applyDelta(-deduction);
        assertThat(account.getBalance()).isGreaterThanOrEqualTo(0L);
      }
    }

    assertThat(account.getBalance()).isGreaterThanOrEqualTo(0L);
  }

  @Provide
  Arbitrary<List<Long>> balancedLines() {
    return Arbitraries.integers()
        .between(1, 100_000)
        .list()
        .ofMinSize(1)
        .ofMaxSize(10)
        .map(
            positiveAmounts -> {
              List<Long> result = new ArrayList<>();
              long total = 0;
              for (int amt : positiveAmounts) {
                result.add((long) amt);
                total += amt;
              }
              result.add(-total);
              return result;
            });
  }

  @Provide
  Arbitrary<List<Long>> unbalancedLines() {
    return Arbitraries.integers()
        .between(1, 50_000)
        .list()
        .ofMinSize(2)
        .ofMaxSize(8)
        .map(
            amounts -> {
              List<Long> result = new ArrayList<>();
              for (int amt : amounts) {
                result.add((long) amt);
              }
              // All positive, sum is strictly > 0
              return result;
            });
  }

  @Provide
  Arbitrary<List<Long>> randomDeductions() {
    return Arbitraries.longs().between(1L, 200_000L).list().ofMinSize(1).ofMaxSize(20);
  }
}
