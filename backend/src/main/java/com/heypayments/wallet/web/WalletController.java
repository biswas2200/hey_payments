package com.heypayments.wallet.web;

import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import com.heypayments.shared.infra.idempotency.IdempotentResult;
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
  @PreAuthorize("hasRole('PAYER')")
  @Operation(summary = "Get current wallet balances")
  public ResponseEntity<WalletResponse> getWallet(Principal principal) {
    UUID userId = UUID.fromString(principal.getName());
    return ResponseEntity.ok(walletService.getWalletResponse(userId));
  }

  @PostMapping("/me/fund")
  @PreAuthorize("hasRole('PAYER')")
  @Operation(summary = "Fund wallet via simulated bank/UPI")
  public ResponseEntity<WalletResponse> fundWallet(
      @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
      @Valid @RequestBody FundRequest request,
      Principal principal) {
    validateUuid(idempotencyKey);
    UUID userId = UUID.fromString(principal.getName());
    IdempotentResult<WalletResponse> result =
        walletService.fundWallet(userId, request.amount(), request.source(), idempotencyKey);
    ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.CREATED);
    if (result.replayed()) {
      builder.header("Idempotent-Replayed", "true");
    }
    return builder.body(result.result());
  }

  private void validateUuid(String key) {
    if (key == null || key.isBlank()) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Idempotency-Key header is required");
    }
    try {
      UUID.fromString(key);
    } catch (IllegalArgumentException e) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "Idempotency-Key must be a valid UUID format");
    }
  }
}
