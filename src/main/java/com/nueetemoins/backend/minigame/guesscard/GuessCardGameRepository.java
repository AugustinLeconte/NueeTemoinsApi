package com.nueetemoins.backend.minigame.guesscard;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GuessCardGameRepository extends JpaRepository<GuessCardGame, UUID> {
    Optional<GuessCardGame> findByUserIdAndPlayedOn(String userId, LocalDate playedOn);
}