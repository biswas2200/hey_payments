package com.heypayments.merchant.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantRepository extends JpaRepository<MerchantEntity, UUID> {

  Optional<MerchantEntity> findByUserId(UUID userId);

  Optional<MerchantEntity> findByHandle(String handle);

  boolean existsByHandle(String handle);
}
