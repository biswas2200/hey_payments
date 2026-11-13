package com.heypayments.wallet.application;

import com.heypayments.ledger.application.LedgerService;
import com.heypayments.ledger.application.PostJournalEntryCommand;
import com.heypayments.ledger.application.PostJournalLineCommand;
import com.heypayments.ledger.domain.model.AccountType;
import com.heypayments.ledger.domain.model.EntryKind;
import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import com.heypayments.shared.infra.id.UuidV7Generator;
import com.heypayments.shared.infra.idempotency.IdempotencyService;
import com.heypayments.wallet.domain.model.Wallet;
import com.heypayments.wallet.infra.persistence.WalletEntity;
import com.heypayments.wallet.infra.persistence.WalletRepository;
import com.heypayments.wallet.web.dto.FundRequest;
import com.heypayments.wallet.web.dto.WalletResponse;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

  public static final long DAILY_FUNDING_LIMIT_PAISE = 5_000_000L; // ₹50,000 in minor units

  private final WalletRepository walletRepository;
  private final LedgerService ledgerService;
  private final IdempotencyService idempotencyService;
  private final JdbcTemplate jdbcTemplate;
  private final Clock clock;

  @Transactional
  public Wallet getOrCreateWallet(UUID userId) {
    Optional<WalletEntity> existing = walletRepository.findByUserId(userId);
    if (existing.isPresent()) {
      return toDomain(existing.get());
    }

    Instant now = clock.instant();
    UUID walletId = UuidV7Generator.generate();
    WalletEntity entity =
        WalletEntity.builder()
            .id(walletId)
            .userId(userId)
            .status("ACTIVE")
            .createdAt(now)
            .updatedAt(now)
            .build();

    WalletEntity saved = walletRepository.save(entity);

    // Initialize USER_MAIN and OFFLINE_ESCROW accounts
    ledgerService.getOrCreateAccount("USER_MAIN:" + walletId, AccountType.USER_MAIN, userId);
    ledgerService.getOrCreateAccount(
        "OFFLINE_ESCROW:" + walletId, AccountType.OFFLINE_ESCROW, userId);

    log.info("Provisioned wallet id={} for userId={}", walletId, userId);
    return toDomain(saved);
  }

  @Transactional(readOnly = true)
  public Wallet getWalletByUserId(UUID userId) {
    return walletRepository
        .findByUserId(userId)
        .map(this::toDomain)
        .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Wallet not found for user: " + userId));
  }

  @Transactional
  public WalletResponse getWalletResponse(UUID userId) {
    Wallet wallet = getOrCreateWallet(userId);
    long mainBalance = ledgerService.getBalance("USER_MAIN:" + wallet.id()).paise();
    long offlineEscrow = ledgerService.getBalance("OFFLINE_ESCROW:" + wallet.id()).paise();

    return new WalletResponse(mainBalance, offlineEscrow, null);
  }

  @Transactional
  public WalletResponse fundWallet(
      UUID userId, long amountPaise, String source, String idempotencyKey) {
    FundRequest payload = new FundRequest(amountPaise, source);
    return idempotencyService.execute(
        idempotencyKey,
        userId,
        payload,
        WalletResponse.class,
        () -> doFundWallet(userId, amountPaise, source, idempotencyKey));
  }

  private WalletResponse doFundWallet(
      UUID userId, long amountPaise, String source, String idempotencyKey) {
    if (amountPaise <= 0L) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Funding amount must be greater than 0");
    }

    Wallet wallet = getOrCreateWallet(userId);

    // Check daily funding limit in UTC
    ZonedDateTime nowUtc = clock.instant().atZone(ZoneOffset.UTC);
    Instant startOfDay = nowUtc.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();

    String userMainCode = "USER_MAIN:" + wallet.id();
    Long fundedToday =
        jdbcTemplate.queryForObject(
            """
            SELECT COALESCE(SUM(jl.amount), 0)
            FROM journal_lines jl
            JOIN journal_entries je ON jl.entry_id = je.id
            JOIN ledger_accounts la ON jl.account_id = la.id
            WHERE la.code = ? AND je.kind = 'FUNDING' AND je.posted_at >= ?
            """,
            Long.class,
            userMainCode,
            Timestamp.from(startOfDay));

    long currentFunded = fundedToday != null ? fundedToday : 0L;
    if (currentFunded + amountPaise > DAILY_FUNDING_LIMIT_PAISE) {
      throw new DomainException(
          ErrorCode.LIMIT_EXCEEDED,
          "Daily funding limit of ₹50,000 exceeded. Currently funded today: ₹"
              + (currentFunded / 100)
              + ", requested: ₹"
              + (amountPaise / 100));
    }

    String ref =
        "fund-"
            + (idempotencyKey != null && !idempotencyKey.isBlank()
                ? idempotencyKey
                : UUID.randomUUID().toString());

    ledgerService.post(
        new PostJournalEntryCommand(
            EntryKind.FUNDING,
            ref,
            "Wallet funding via " + (source != null ? source : "SIM_BANK"),
            List.of(
                new PostJournalLineCommand("SYS:BANK_SIM_CLEARING", -amountPaise),
                new PostJournalLineCommand(userMainCode, amountPaise))));

    return getWalletResponse(userId);
  }

  private Wallet toDomain(WalletEntity entity) {
    return new Wallet(
        entity.getId(),
        entity.getUserId(),
        entity.getStatus(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
