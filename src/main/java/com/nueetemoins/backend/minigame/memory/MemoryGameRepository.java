package com.nueetemoins.backend.minigame.memory;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoryGameRepository extends JpaRepository<MemoryGame, UUID> {
    Optional<MemoryGame> findByUserIdAndPlayedOn(String userId, LocalDate playedOn);
}