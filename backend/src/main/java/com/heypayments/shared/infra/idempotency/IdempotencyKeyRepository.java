package com.heypayments.shared.infra.idempotency;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, String> {
  Optional<IdempotencyKeyEntity> findByKey(String key);
}
