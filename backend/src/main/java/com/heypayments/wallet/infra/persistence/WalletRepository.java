package com.heypayments.wallet.infra.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends JpaRepository<WalletEntity, UUID> {
  Optional<WalletEntity> findByUserId(UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT w FROM WalletEntity w WHERE w.userId = :userId")
  Optional<WalletEntity> findByUserIdForUpdate(@Param("userId") UUID userId);

  boolean existsByUserId(UUID userId);
}
