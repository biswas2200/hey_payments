package com.heypayments.ledger.application;

import com.heypayments.ledger.domain.model.EntryKind;
import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import java.util.List;

public record PostJournalEntryCommand(
    EntryKind kind, String reference, String memo, List<PostJournalLineCommand> lines) {

  public void validate() {
    if (lines == null || lines.size() < 2) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "Journal entry must have at least 2 lines");
    }

    long sum = 0L;
    for (PostJournalLineCommand line : lines) {
      if (line.amount() == 0L) {
        throw new DomainException(ErrorCode.VALIDATION_FAILED, "Journal line amount cannot be 0");
      }
      try {
        sum = Math.addExact(sum, line.amount());
      } catch (ArithmeticException e) {
        throw new DomainException(
            ErrorCode.VALIDATION_FAILED, "Journal lines sum overflowed 64-bit integer range");
      }
    }

    if (sum != 0L) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED,
          "Journal entry unbalanced: sum of lines must be 0, found " + sum);
    }
  }
}
