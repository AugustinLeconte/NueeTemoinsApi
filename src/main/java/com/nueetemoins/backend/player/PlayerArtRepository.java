package com.nueetemoins.backend.player;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerArtRepository extends JpaRepository<PlayerArt, UUID> {

    @EntityGraph(attributePaths = {"art", "art.card"})
    List<PlayerArt> findByUserId(String userId);

    boolean existsByUserIdAndArtId(String userId, UUID artId);

    boolean existsByArtId(UUID artId);

    @Query("select pa.art.id from PlayerArt pa where pa.userId = :userId")
    Set<UUID> findOwnedArtIds(@Param("userId") String userId);
}
