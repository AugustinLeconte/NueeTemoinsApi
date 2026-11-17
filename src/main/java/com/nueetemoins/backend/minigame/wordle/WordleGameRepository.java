package com.nueetemoins.backend.minigame.wordle;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WordleGameRepository extends JpaRepository<WordleGame, UUID> {

    Optional<WordleGame> findByUserIdAndGameDate(String userId, LocalDate gameDate);

    /** XP décroissant = moins de coups d'abord (l'XP est monotone avec la perf), puis le plus rapide. */
    @Query("""
        select g from WordleGame g
        where g.gameDate = :date and g.lang = :lang and g.finishedAt is not null
        order by g.xpAwarded desc, g.finishedAt asc
        """)
    List<WordleGame> findLeaderboard(@Param("date") LocalDate date,
                                     @Param("lang") String lang,
                                     Pageable pageable);
}