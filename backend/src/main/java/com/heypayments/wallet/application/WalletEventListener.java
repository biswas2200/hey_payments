package com.heypayments.wallet.application;

import com.heypayments.auth.domain.event.UserRegisteredEvent;
import com.heypayments.auth.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletEventListener {

  private final WalletService walletService;

  @EventListener
  public void onUserRegistered(UserRegisteredEvent event) {
    if (event.role() == UserRole.PAYER) {
      log.info("Handling UserRegisteredEvent for PAYER userId={}", event.userId());
      walletService.getOrCreateWallet(event.userId());
    }
  }
}
