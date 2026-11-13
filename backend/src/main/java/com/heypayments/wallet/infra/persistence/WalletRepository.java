package com.heypayments.wallet.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends JpaRepository<WalletEntity, UUID> {
  Optional<WalletEntity> findByUserId(UUID userId);
  boolean existsByUserId(UUID userId);
}
