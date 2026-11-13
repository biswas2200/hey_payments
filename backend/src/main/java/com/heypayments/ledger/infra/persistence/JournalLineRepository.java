package com.heypayments.ledger.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLineEntity, UUID> {

  List<JournalLineEntity> findByEntryId(UUID entryId);
}
