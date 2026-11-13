package com.heypayments.shared.infra.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heypayments.shared.domain.exception.DomainException;
import com.heypayments.shared.domain.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

  private final IdempotencyKeyRepository repository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  @Transactional
  public <T> T execute(
      String key, UUID userId, Object requestPayload, Class<T> responseType, Supplier<T> action) {
    if (key == null || key.isBlank()) {
      return action.get();
    }

    String requestHash = hashPayload(requestPayload);

    Optional<IdempotencyKeyEntity> existing = repository.findByKey(key);
    if (existing.isPresent()) {
      IdempotencyKeyEntity entity = existing.get();
      if (!requestHash.equals(entity.getRequestHash())) {
        throw new DomainException(
            ErrorCode.IDEMPOTENCY_KEY_REUSED,
            "Idempotency key was already used with a different request payload");
      }

      try {
        return objectMapper.readValue(entity.getResponse(), responseType);
      } catch (JsonProcessingException e) {
        log.error("Failed to deserialize cached idempotency response: {}", e.getMessage());
        throw new DomainException(ErrorCode.INTERNAL, "Corrupted cached response");
      }
    }

    T response = action.get();

    String responseJson;
    try {
      responseJson = objectMapper.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize idempotency response: {}", e.getMessage());
      responseJson = "{}";
    }

    Instant now = clock.instant();
    IdempotencyKeyEntity newKey =
        IdempotencyKeyEntity.builder()
            .key(key)
            .userId(userId)
            .requestHash(requestHash)
            .response(responseJson)
            .status("COMPLETED")
            .createdAt(now)
            .build();

    repository.save(newKey);
    return response;
  }

  private String hashPayload(Object payload) {
    if (payload == null) {
      return "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"; // sha256("")
    }
    try {
      byte[] jsonBytes = objectMapper.writeValueAsBytes(payload);
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(jsonBytes);
      return HexFormat.of().formatHex(digest);
    } catch (JsonProcessingException | NoSuchAlgorithmException e) {
      log.error("Failed to hash idempotency payload: {}", e.getMessage());
      return "fallback-" + payload.hashCode();
    }
  }
}
