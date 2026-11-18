package com.nueetemoins.backend.minigame;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, UUID> {
    Optional<DailyChallenge> findByGameAndDay(MiniGameType game, LocalDate day);
}