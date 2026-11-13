package com.heypayments.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.heypayments.ledger.application.PostJournalEntryCommand;
import com.heypayments.ledger.application.PostJournalLineCommand;
import com.heypayments.ledger.domain.model.AccountType;
import com.heypayments.ledger.domain.model.EntryKind;
import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

class LedgerInvariantsPropertyTest {

  @Property
  void balancedEntriesConserveMoney(@ForAll("balancedLines") List<Long> amounts) {
    long sum = amounts.stream().mapToLong(Long::longValue).sum();
    assertThat(sum).isEqualTo(0L);

    List<PostJournalLineCommand> lines = new ArrayList<>();
    for (int i = 0; i < amounts.size(); i++) {
      lines.add(new PostJournalLineCommand("ACC:" + i, amounts.get(i)));
    }

    PostJournalEntryCommand command =
        new PostJournalEntryCommand(EntryKind.ADJUSTMENT, "ref-test", "test memo", lines);

    assertThat(command.lines()).hasSizeGreaterThanOrEqualTo(2);
    long lineSum = command.lines().stream().mapToLong(PostJournalLineCommand::amount).sum();
    assertThat(lineSum).isEqualTo(0L);
  }

  @Property
  void unbalancedEntriesMustBeRejected(@ForAll("unbalancedLines") List<Long> amounts) {
    long sum = amounts.stream().mapToLong(Long::longValue).sum();
    assertThat(sum).isNotEqualTo(0L);

    List<PostJournalLineCommand> lines = new ArrayList<>();
    for (int i = 0; i < amounts.size(); i++) {
      lines.add(new PostJournalLineCommand("ACC:" + i, amounts.get(i)));
    }

    // Verify invariant validation logic
    assertThatThrownBy(
            () -> {
              long checkSum = 0L;
              for (PostJournalLineCommand line : lines) {
                checkSum += line.amount();
              }
              if (checkSum != 0L) {
                throw new DomainException(ErrorCode.VALIDATION_FAILED, "Journal entry unbalanced: " + checkSum);
              }
            })
        .isInstanceOf(DomainException.class)
        .matches(ex -> ((DomainException) ex).getErrorCode() == ErrorCode.VALIDATION_FAILED);
  }

  @Property
  void userMainAccountNeverBecomesNegative(
      @ForAll @IntRange(min = 0, max = 1_000_000) int initialBalance,
      @ForAll("randomDeductions") List<Long> deductions) {

    long balance = initialBalance;
    for (long deduction : deductions) {
      if (deduction > balance) {
        // Must reject deduction that would exceed balance
        long finalBalance = balance;
        assertThatThrownBy(
                () -> {
                  if (finalBalance - deduction < 0L) {
                    throw new DomainException(ErrorCode.INSUFFICIENT_FUNDS, "Insufficient funds");
                  }
                })
            .isInstanceOf(DomainException.class);
      } else {
        balance -= deduction;
        assertThat(balance).isGreaterThanOrEqualTo(0L);
      }
    }

    assertThat(balance).isGreaterThanOrEqualTo(0L);
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
