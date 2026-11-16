package com.nueetemoins.backend.player;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerPackClaimRepository extends JpaRepository<PlayerPackClaim, UUID> {

    boolean existsByUserIdAndLevel(String userId, int level);

    long countByUserId(String userId);

    @Query("select c.level from PlayerPackClaim c where c.userId = :userId")
    Set<Integer> findLevelsByUserId(@Param("userId") String userId);
}
