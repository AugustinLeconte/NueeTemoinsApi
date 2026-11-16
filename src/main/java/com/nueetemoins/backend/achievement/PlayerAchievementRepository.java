package com.nueetemoins.backend.achievement;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerAchievementRepository extends JpaRepository<PlayerAchievement, UUID> {

    List<PlayerAchievement> findByUserId(String userId);

    @Query("select a.code from PlayerAchievement a where a.userId = :userId")
    Set<String> findCodesByUserId(@Param("userId") String userId);
}