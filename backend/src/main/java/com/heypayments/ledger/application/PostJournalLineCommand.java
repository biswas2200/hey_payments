package com.heypayments.ledger.application;

public record PostJournalLineCommand(String accountCode, long amount) {}
