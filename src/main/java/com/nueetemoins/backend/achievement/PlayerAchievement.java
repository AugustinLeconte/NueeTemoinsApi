package com.nueetemoins.backend.achievement;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Succès débloqué par un joueur. */
@Entity
@Table(name = "player_achievements",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "code"}))
@Getter @Setter @NoArgsConstructor
public class PlayerAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String code;

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt = Instant.now();
}