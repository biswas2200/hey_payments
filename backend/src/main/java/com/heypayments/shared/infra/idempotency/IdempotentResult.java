package com.heypayments.shared.infra.idempotency;

public record IdempotentResult<T>(T result, boolean replayed) {}
