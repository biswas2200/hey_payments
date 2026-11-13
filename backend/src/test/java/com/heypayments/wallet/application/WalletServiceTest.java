package com.heypayments.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.heypayments.BaseIntegrationTest;
import com.heypayments.auth.domain.event.UserRegisteredEvent;
import com.heypayments.auth.domain.model.UserRole;
import com.heypayments.ledger.application.LedgerService;
import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import com.heypayments.wallet.domain.model.Wallet;
import com.heypayments.wallet.web.dto.WalletResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

class WalletServiceTest extends BaseIntegrationTest {

  @Autowired private WalletService walletService;
  @Autowired private LedgerService ledgerService;
  @Autowired private ApplicationEventPublisher eventPublisher;
  @Autowired private JdbcTemplate jdbcTemplate;

  private UUID createRawUser() {
    UUID userId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO users (id, phone, email, password_hash, role, status, created_at, updated_at) VALUES (?, ?, ?, 'hash', 'PAYER', 'ACTIVE', NOW(), NOW())",
        userId,
        "+9198" + UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 8),
        "user-" + userId + "@example.com");
    return userId;
  }

  @Test
  @DisplayName("Payer registration event automatically provisions wallet and USER_MAIN ledger account")
  void shouldProvisionWalletOnPayerRegistrationEvent() {
    UUID userId = createRawUser();
    eventPublisher.publishEvent(new UserRegisteredEvent(userId, UserRole.PAYER, Instant.now()));

    Wallet wallet = walletService.getWalletByUserId(userId);
    assertThat(wallet).isNotNull();
    assertThat(wallet.userId()).isEqualTo(userId);
    assertThat(wallet.status()).isEqualTo("ACTIVE");

    WalletResponse response = walletService.getWalletResponse(userId);
    assertThat(response.mainBalance()).isEqualTo(0L);
    assertThat(response.offlineEscrow()).isEqualTo(0L);
    assertThat(response.activeBc()).isNull();

    // Verify ledger accounts were created
    assertThat(ledgerService.getBalance("USER_MAIN:" + wallet.id()).paise()).isEqualTo(0L);
    assertThat(ledgerService.getBalance("OFFLINE_ESCROW:" + wallet.id()).paise()).isEqualTo(0L);
  }

  @Test
  @DisplayName("Fund wallet credits USER_MAIN account from SYS:BANK_SIM_CLEARING")
  void shouldFundWalletSuccessfully() {
    UUID userId = createRawUser();
    Wallet wallet = walletService.getOrCreateWallet(userId);

    String key = "fund-test-" + UUID.randomUUID();
    WalletResponse response = walletService.fundWallet(userId, 5000L, "SIM_BANK", key);

    assertThat(response.mainBalance()).isEqualTo(5000L);
    assertThat(ledgerService.getBalance("USER_MAIN:" + wallet.id()).paise()).isEqualTo(5000L);
  }

  @Test
  @DisplayName("Daily funding cap of ₹50,000 (5,000,000 paise) is strictly enforced")
  void shouldEnforceDailyFundingLimit() {
    UUID userId = createRawUser();
    walletService.getOrCreateWallet(userId);

    // 1. Fund 40,000 INR (4,000,000 paise) -> succeeds
    walletService.fundWallet(userId, 4_000_000L, "SIM_BANK", "fund-1-" + UUID.randomUUID());

    // 2. Try to fund another 15,000 INR (1,500,000 paise) -> exceeds 50,000 daily cap
    assertThatThrownBy(
            () ->
                walletService.fundWallet(
                    userId, 1_500_000L, "SIM_BANK", "fund-2-" + UUID.randomUUID()))
        .isInstanceOf(DomainException.class)
        .satisfies(
            ex -> {
              DomainException de = (DomainException) ex;
              assertThat(de.getErrorCode()).isEqualTo(ErrorCode.LIMIT_EXCEEDED);
            });

    // Main balance remains 40,000 INR
    WalletResponse response = walletService.getWalletResponse(userId);
    assertThat(response.mainBalance()).isEqualTo(4_000_000L);
  }

  @Test
  @DisplayName("Fund wallet is idempotent on Idempotency-Key")
  void shouldBeIdempotentOnFunding() {
    UUID userId = createRawUser();
    walletService.getOrCreateWallet(userId);

    String idempotencyKey = "idemp-" + UUID.randomUUID();
    WalletResponse res1 = walletService.fundWallet(userId, 1000L, "SIM_BANK", idempotencyKey);
    WalletResponse res2 = walletService.fundWallet(userId, 1000L, "SIM_BANK", idempotencyKey);

    assertThat(res1.mainBalance()).isEqualTo(1000L);
    assertThat(res2.mainBalance()).isEqualTo(1000L);
  }
}
