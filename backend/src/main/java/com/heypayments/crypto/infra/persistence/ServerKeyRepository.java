package com.heypayments.crypto.infra.persistence;

import com.heypayments.crypto.domain.model.ServerKeyStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerKeyRepository extends JpaRepository<ServerKeyEntity, String> {

  Optional<ServerKeyEntity> findFirstByStatusOrderByCreatedAtDesc(ServerKeyStatus status);

  List<ServerKeyEntity> findAllByStatusIn(Collection<ServerKeyStatus> statuses);
}
