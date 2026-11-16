package com.nueetemoins.backend.player.dto;

import java.util.List;

public record WalletResponse(int coins, int packsSinceRare, boolean dailyPackAvailable, List<String> newAchievements) {}
