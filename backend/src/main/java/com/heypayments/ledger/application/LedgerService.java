package com.heypayments.ledger.application;

import com.heypayments.ledger.domain.model.AccountType;
import com.heypayments.ledger.domain.model.JournalEntry;
import com.heypayments.ledger.domain.model.JournalLine;
import com.heypayments.ledger.domain.model.LedgerAccount;
import com.heypayments.ledger.infra.persistence.JournalEntryEntity;
import com.heypayments.ledger.infra.persistence.JournalEntryRepository;
import com.heypayments.ledger.infra.persistence.JournalLineEntity;
import com.heypayments.ledger.infra.persistence.JournalLineRepository;
import com.heypayments.ledger.infra.persistence.LedgerAccountEntity;
import com.heypayments.ledger.infra.persistence.LedgerAccountRepository;
import com.heypayments.shared.infra.id.UuidV7Generator;
import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import com.heypayments.shared.domain.model.Money;
import com.heypayments.shared.infra.audit.AuditService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

  private final LedgerAccountRepository accountRepository;
  private final JournalEntryRepository entryRepository;
  private final JournalLineRepository lineRepository;
  private final AuditService auditService;
  private final Clock clock;

  @Transactional
  public LedgerAccount getOrCreateAccount(String code, AccountType type, UUID ownerId) {
    Optional<LedgerAccountEntity> existing = accountRepository.findByCode(code);
    if (existing.isPresent()) {
      return toDomain(existing.get());
    }

    Instant now = clock.instant();
    LedgerAccountEntity entity =
        LedgerAccountEntity.builder()
            .id(UuidV7Generator.generate())
            .code(code)
            .type(type)
            .ownerId(ownerId)
            .currency("INR")
            .balance(0L)
            .createdAt(now)
            .updatedAt(now)
            .build();

    LedgerAccountEntity saved = accountRepository.save(entity);
    return toDomain(saved);
  }

  @Transactional(readOnly = true)
  public LedgerAccount getAccountByCode(String code) {
    return accountRepository
        .findByCode(code)
        .map(this::toDomain)
        .orElseThrow(
            () -> new DomainException(ErrorCode.NOT_FOUND, "Ledger account not found: " + code));
  }

  @Transactional(readOnly = true)
  public Money getBalance(String code) {
    long paise = getAccountByCode(code).balancePaise();
    if (paise < 0) {
      throw new DomainException(
          ErrorCode.INTERNAL,
          "Account " + code + " has negative balance " + paise + ", cannot represent as Money");
    }
    return Money.ofPaise(paise);
  }

  @Transactional(readOnly = true)
  public long getBalancePaise(String code) {
    return getAccountByCode(code).balancePaise();
  }

  @Transactional
  public JournalEntry post(PostJournalEntryCommand command) {
    // 1. Idempotent check
    Optional<JournalEntryEntity> existing = entryRepository.findByReference(command.reference());
    if (existing.isPresent()) {
      return getExistingEntry(existing.get());
    }

    // 2. Validate line constraints and balance invariant (Σ = 0)
    List<PostJournalLineCommand> lines = command.lines();
    if (lines == null || lines.size() < 2) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "Journal entry must have at least 2 lines");
    }

    long sum = 0L;
    for (PostJournalLineCommand line : lines) {
      if (line.amount() == 0L) {
        throw new DomainException(ErrorCode.VALIDATION_FAILED, "Journal line amount cannot be 0");
      }
      sum += line.amount();
    }

    if (sum != 0L) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED,
          "Journal entry unbalanced: sum of lines must be 0, found " + sum);
    }

    Instant now = clock.instant();
    UUID entryId = UuidV7Generator.generate();

    // 3. Update account balances with non-negative checks for user accounts
    Map<String, LedgerAccountEntity> accountCache = new HashMap<>();
    List<JournalLineEntity> lineEntities = new ArrayList<>(lines.size());
    List<JournalLine> domainLines = new ArrayList<>(lines.size());

    for (PostJournalLineCommand line : lines) {
      LedgerAccountEntity account =
          accountCache.computeIfAbsent(
              line.accountCode(),
              code ->
                  accountRepository
                      .findByCode(code)
                      .orElseThrow(
                          () ->
                              new DomainException(
                                  ErrorCode.NOT_FOUND, "Account not found: " + code)));

      long newBalance = account.getBalance() + line.amount();
      if (isNonNegativeAccountType(account.getType()) && newBalance < 0L) {
        throw new DomainException(
            ErrorCode.INSUFFICIENT_FUNDS,
            "Account " + account.getCode() + " cannot have negative balance: " + newBalance);
      }

      account.setBalance(newBalance);
      account.setUpdatedAt(now);

      UUID lineId = UuidV7Generator.generate();
      lineEntities.add(
          JournalLineEntity.builder()
              .id(lineId)
              .entryId(entryId)
              .accountId(account.getId())
              .amount(line.amount())
              .createdAt(now)
              .build());

      domainLines.add(
          new JournalLine(lineId, account.getId(), account.getCode(), line.amount(), now));
    }

    // Save modified accounts (optimistic locking enforced via @Version)
    accountRepository.saveAll(accountCache.values());

    // 4. Save journal entry and lines
    JournalEntryEntity entryEntity =
        JournalEntryEntity.builder()
            .id(entryId)
            .kind(command.kind())
            .reference(command.reference())
            .memo(command.memo())
            .postedAt(now)
            .build();
    entryRepository.save(entryEntity);
    lineRepository.saveAll(lineEntities);

    // 5. Append-only audit log
    auditService.log(
        null,
        "POST_JOURNAL_ENTRY",
        "JOURNAL_ENTRY",
        entryId,
        Map.of(
            "kind",
            command.kind().name(),
            "reference",
            command.reference(),
            "memo",
            command.memo() != null ? command.memo() : "",
            "lineCount",
            lines.size(),
            "sum",
            0L),
        null);

    log.info("Posted journal entry id={} kind={} ref={}", entryId, command.kind(), command.reference());

    return new JournalEntry(
        entryId, command.kind(), command.reference(), now, command.memo(), domainLines);
  }

  private boolean isNonNegativeAccountType(AccountType type) {
    return type == AccountType.USER_MAIN
        || type == AccountType.OFFLINE_ESCROW
        || type == AccountType.MERCHANT_PAYABLE;
  }

  private JournalEntry getExistingEntry(JournalEntryEntity entity) {
    List<JournalLineEntity> lines = lineRepository.findByEntryId(entity.getId());
    List<JournalLine> domainLines = new ArrayList<>(lines.size());
    for (JournalLineEntity l : lines) {
      String code =
          accountRepository.findById(l.getAccountId()).map(LedgerAccountEntity::getCode).orElse("");
      domainLines.add(
          new JournalLine(l.getId(), l.getAccountId(), code, l.getAmount(), l.getCreatedAt()));
    }
    return new JournalEntry(
        entity.getId(),
        entity.getKind(),
        entity.getReference(),
        entity.getPostedAt(),
        entity.getMemo(),
        domainLines);
  }

  private LedgerAccount toDomain(LedgerAccountEntity entity) {
    return new LedgerAccount(
        entity.getId(),
        entity.getCode(),
        entity.getType(),
        entity.getOwnerId(),
        entity.getCurrency(),
        entity.getBalance(),
        entity.getVersion() != null ? entity.getVersion() : 0L,
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
