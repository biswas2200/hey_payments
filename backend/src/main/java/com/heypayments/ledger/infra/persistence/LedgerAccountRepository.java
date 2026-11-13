package com.heypayments.ledger.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccountEntity, UUID> {

  Optional<LedgerAccountEntity> findByCode(String code);

  List<LedgerAccountEntity> findByOwnerId(UUID ownerId);
}
