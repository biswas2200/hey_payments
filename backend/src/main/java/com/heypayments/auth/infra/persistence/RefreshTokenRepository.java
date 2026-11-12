package com.heypayments.auth.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

  Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

  List<RefreshTokenEntity> findAllByFamilyId(UUID familyId);

  @Modifying
  @Query(
      "UPDATE RefreshTokenEntity r SET r.revokedAt = CURRENT_TIMESTAMP WHERE r.familyId = :familyId AND r.revokedAt IS NULL")
  int revokeAllByFamilyId(@Param("familyId") UUID familyId);
}
