package com.nueetemoins.backend.card;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardArtRepository extends JpaRepository<CardArt, UUID> {

    Optional<CardArt> findByPublicId(String publicId);
    boolean existsByPublicId(String publicId);
    boolean existsByArtistId(UUID artistId);
    Optional<CardArt> findByCardIdAndSource(UUID cardId, ArtSource source);

    /** Illustrations tirables dans un pack. */
    @Query("""
        select a from CardArt a join fetch a.card
        where a.obtainable = true and a.dropWeight > 0 and a.source in :sources
        """)
    List<CardArt> findPackPool(@Param("sources") Collection<ArtSource> sources);

    @Query("select a from CardArt a join fetch a.card where a.source = :source and a.obtainable = true")
    List<CardArt> findBySourceWithCard(@Param("source") ArtSource source);
}
