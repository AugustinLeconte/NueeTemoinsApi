package com.nueetemoins.backend.profile.dto;

import java.util.List;

import com.nueetemoins.backend.achievement.dto.AchievementResponse;

public record PublicProfileResponse(String pseudo, String country, int level,
                                    Patron patron, List<AchievementResponse> achievements) {
    public record Patron(String cardId, String name, String imageUrl) {}
}
