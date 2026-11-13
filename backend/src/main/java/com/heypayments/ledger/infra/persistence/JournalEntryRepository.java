package com.heypayments.ledger.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntryEntity, UUID> {

  Optional<JournalEntryEntity> findByReference(String reference);

  boolean existsByReference(String reference);
}
