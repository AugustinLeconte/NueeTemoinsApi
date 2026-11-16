package com.nueetemoins.backend.player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerCardRepository extends JpaRepository<PlayerCard, UUID> {

    @EntityGraph(attributePaths = {"card", "equippedArt"})
    List<PlayerCard> findByUserId(String userId);

    @EntityGraph(attributePaths = {"card", "equippedArt"})
    Optional<PlayerCard> findByUserIdAndCardPublicId(String userId, String cardPublicId);

    Optional<PlayerCard> findByUserIdAndCardId(String userId, UUID cardId);

    boolean existsByCardId(UUID cardId);

    long countByUserId(String userId);
}
