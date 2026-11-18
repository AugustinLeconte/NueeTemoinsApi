package com.nueetemoins.backend.player;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerPackClaimRepository extends JpaRepository<PlayerPackClaim, UUID> {

    Optional<PlayerPackClaim> findByUserIdAndLevel(String userId, int level);

    boolean existsByUserIdAndLevel(String userId, int level);

    long countByUserId(String userId);

    long countByUserIdAndOpenedTrue(String userId);

    @Query("select c.level from PlayerPackClaim c where c.userId = :userId")
    Set<Integer> findLevelsByUserId(@Param("userId") String userId);

    @Query("select c.level from PlayerPackClaim c where c.userId = :userId and c.opened = true")
    Set<Integer> findOpenedLevelsByUserId(@Param("userId") String userId);

    @Query("select c.level from PlayerPackClaim c where c.userId = :userId and c.opened = false")
    Set<Integer> findStoredLevelsByUserId(@Param("userId") String userId);
}
