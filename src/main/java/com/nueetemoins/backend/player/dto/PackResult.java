package com.nueetemoins.backend.player.dto;

import java.util.List;

public record PackResult(
    String packType, List<DrawnItem> draws,
    int coinsGained, int coins, int packsSinceRare,
    int xpGained, int levelBefore, ProgressResponse progress
) {}
