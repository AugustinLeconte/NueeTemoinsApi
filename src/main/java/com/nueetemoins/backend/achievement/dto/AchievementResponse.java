package com.nueetemoins.backend.achievement.dto;

import java.time.Instant;

public record AchievementResponse(String code, boolean unlocked, Instant unlockedAt, int progress, int target) {}