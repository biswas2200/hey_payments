package com.heypayments.wallet.web;

import com.heypayments.wallet.application.WalletService;
import com.heypayments.wallet.web.dto.FundRequest;
import com.heypayments.wallet.web.dto.WalletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "wallet", description = "Payer wallet management and funding simulator")
public class WalletController {

  private final WalletService walletService;

  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get current wallet balances")
  public ResponseEntity<WalletResponse> getWallet(Principal principal) {
    UUID userId = UUID.fromString(principal.getName());
    return ResponseEntity.ok(walletService.getWalletResponse(userId));
  }

  @PostMapping("/me/fund")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Fund wallet via simulated bank/UPI")
  public ResponseEntity<WalletResponse> fundWallet(
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody FundRequest request,
      Principal principal) {
    UUID userId = UUID.fromString(principal.getName());
    WalletResponse response =
        walletService.fundWallet(userId, request.amount(), request.source(), idempotencyKey);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
