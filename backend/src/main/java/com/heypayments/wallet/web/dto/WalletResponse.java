package com.heypayments.wallet.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WalletResponse(Long mainBalance, Long offlineEscrow, Object activeBc) {}
