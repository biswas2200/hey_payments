package com.heypayments.shared.infra.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heypayments.BaseIntegrationTest;
import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class IdempotencyServiceTest extends BaseIntegrationTest {

  @Autowired private IdempotencyKeyRepository repository;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private Clock clock;

  record SamplePayload(String name, int amount) {}

  record SampleResponse(String status, int balance) {}

  @Test
  @DisplayName(
      "should execute action on first call and return replayed=false, then replay on second call with replayed=true")
  void shouldExecuteAndReplayResult() {
    IdempotencyService service = new IdempotencyService(repository, objectMapper, clock);
    String key = UUID.randomUUID().toString();
    UUID userId = UUID.randomUUID();
    SamplePayload payload = new SamplePayload("item", 100);

    IdempotentResult<SampleResponse> first =
        service.execute(
            key, userId, payload, SampleResponse.class, () -> new SampleResponse("OK", 500));

    assertThat(first.replayed()).isFalse();
    assertThat(first.result().balance()).isEqualTo(500);

    // Second call with same key, user, payload -> returns cached response with replayed=true
    IdempotentResult<SampleResponse> second =
        service.execute(
            key,
            userId,
            payload,
            SampleResponse.class,
            () -> new SampleResponse("SHOULD_NOT_BE_CALLED", 9999));

    assertThat(second.replayed()).isTrue();
    assertThat(second.result().balance()).isEqualTo(500);
  }

  @Test
  @DisplayName("HIGH-2: should reject cross-user idempotency key collision to prevent IDOR")
  void shouldRejectCrossUserKeyCollision() {
    IdempotencyService service = new IdempotencyService(repository, objectMapper, clock);
    String key = UUID.randomUUID().toString();
    UUID userA = UUID.randomUUID();
    UUID userB = UUID.randomUUID();
    SamplePayload payload = new SamplePayload("item", 100);

    // User A executes first
    service.execute(
        key, userA, payload, SampleResponse.class, () -> new SampleResponse("OK", 1000));

    // User B attempts to execute with User A's key
    assertThatThrownBy(
            () ->
                service.execute(
                    key,
                    userB,
                    payload,
                    SampleResponse.class,
                    () -> new SampleResponse("OK", 2000)))
        .isInstanceOf(DomainException.class)
        .satisfies(
            ex -> {
              DomainException de = (DomainException) ex;
              assertThat(de.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
              assertThat(de.getMessage()).contains("does not belong to the authenticated user");
            });
  }
}
