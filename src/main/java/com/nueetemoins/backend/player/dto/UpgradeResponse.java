package com.nueetemoins.backend.player.dto;

import java.util.List;

public record UpgradeResponse(
    int level, int fragments, Integer nextUpgradeCost,
    int xpGained, boolean playerLevelUp, ProgressResponse progress, List<String> newAchievements
) {}
