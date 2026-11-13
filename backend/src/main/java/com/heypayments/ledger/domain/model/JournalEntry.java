package com.heypayments.ledger.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JournalEntry(
    UUID id,
    EntryKind kind,
    String reference,
    Instant postedAt,
    String memo,
    List<JournalLine> lines) {}
