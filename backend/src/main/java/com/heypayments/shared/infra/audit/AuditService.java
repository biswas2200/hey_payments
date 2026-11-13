package com.heypayments.shared.infra.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heypayments.shared.infra.id.UuidV7Generator;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

  private final AuditLogRepository auditLogRepository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  @Transactional(propagation = Propagation.MANDATORY)
  public void log(
      UUID actorId, String action, String entity, String entityId, Object data, String ip) {
    Instant now = clock.instant();
    String jsonData = null;
    if (data != null) {
      try {
        jsonData = objectMapper.writeValueAsString(data);
      } catch (JsonProcessingException e) {
        log.warn("Failed to serialize audit log data: {}", e.getMessage());
        jsonData = "{}";
      }
    }

    AuditLogEntity auditEntity =
        AuditLogEntity.builder()
            .id(UuidV7Generator.generate())
            .actorId(actorId)
            .action(action)
            .entity(entity)
            .entityId(entityId)
            .data(jsonData)
            .ip(ip)
            .at(now)
            .build();

    auditLogRepository.save(auditEntity);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void log(
      UUID actorId, String action, String entity, UUID entityId, Object data, String ip) {
    log(actorId, action, entity, entityId != null ? entityId.toString() : null, data, ip);
  }
}
