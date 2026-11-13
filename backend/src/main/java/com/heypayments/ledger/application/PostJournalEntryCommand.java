package com.heypayments.ledger.application;

import com.heypayments.ledger.domain.model.EntryKind;
import java.util.List;

public record PostJournalEntryCommand(
    EntryKind kind, String reference, String memo, List<PostJournalLineCommand> lines) {}
