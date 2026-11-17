package com.nueetemoins.backend.minigame;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MiniGameResultRepository extends JpaRepository<MiniGameResult, UUID> {

    long countByUserIdAndWonTrue(String userId);

    long countByUserIdAndPerfectTrue(String userId);

    @Query("select count(distinct r.game) from MiniGameResult r where r.userId = :userId and r.won = true")
    long countDistinctGamesWon(@Param("userId") String userId);

    /** Jours où le joueur a terminé au moins une partie, du plus récent au plus ancien. */
    @Query("select distinct r.playedOn from MiniGameResult r where r.userId = :userId order by r.playedOn desc")
    List<LocalDate> findPlayedDays(@Param("userId") String userId);
}