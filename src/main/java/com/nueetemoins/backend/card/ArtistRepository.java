package com.nueetemoins.backend.card;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, UUID> {
    Optional<Artist> findByPublicId(String publicId);
    boolean existsByPublicId(String publicId);
}
