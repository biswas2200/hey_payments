package com.heypayments.merchant.application;

import com.heypayments.merchant.domain.model.Merchant;
import com.heypayments.merchant.domain.model.MerchantStatus;
import com.heypayments.merchant.infra.persistence.MerchantEntity;
import com.heypayments.merchant.infra.persistence.MerchantRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MerchantService {

  private final MerchantRepository merchantRepository;
  private final Clock clock;

  @Transactional(readOnly = true)
  public Optional<Merchant> findByUserId(UUID userId) {
    return merchantRepository.findByUserId(userId).map(this::toDomain);
  }

  @Transactional(readOnly = true)
  public boolean existsByHandle(String handle) {
    return merchantRepository.existsByHandle(handle);
  }

  @Transactional
  public Merchant createMerchant(
      UUID userId, String displayName, String handle, String category, long pendingCap) {
    Instant now = clock.instant();
    MerchantEntity merchant =
        MerchantEntity.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .displayName(displayName)
            .handle(handle)
            .category(category)
            .status(MerchantStatus.ACTIVE)
            .pendingCap(pendingCap)
            .createdAt(now)
            .updatedAt(now)
            .build();
    MerchantEntity saved = merchantRepository.save(merchant);
    return toDomain(saved);
  }

  private Merchant toDomain(MerchantEntity entity) {
    return new Merchant(
        entity.getId(),
        entity.getUserId(),
        entity.getDisplayName(),
        entity.getHandle(),
        entity.getCategory(),
        entity.getStatus(),
        entity.getPendingCap(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
