package com.nueetemoins.backend.card;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, UUID> {
    Optional<Card> findByPublicId(String publicId);
    boolean existsByPublicId(String publicId);
}