package com.nueetemoins.backend.player.dto;

public record UpgradeResponse(
    int level, int fragments, Integer nextUpgradeCost,
    int xpGained, boolean playerLevelUp, ProgressResponse progress
) {}
