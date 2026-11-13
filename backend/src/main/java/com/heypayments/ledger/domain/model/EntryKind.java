package com.heypayments.ledger.domain.model;

public enum EntryKind {
  FUNDING,
  LOAD_OFFLINE,
  UNLOAD_OFFLINE,
  SETTLEMENT,
  REVERSAL,
  FEE,
  ADJUSTMENT
}
