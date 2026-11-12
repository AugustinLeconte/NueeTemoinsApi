package com.nueetemoins.backend.family;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyRepository extends JpaRepository<Family, UUID> {
    Optional<Family> findByPublicId(String publicId);
    boolean existsByPublicId(String publicId);
    List<Family> findByPublicIdIn(Collection<String> publicIds);
}