package com.nueetemoins.backend.player.dto;

import java.util.List;

public record PackResult(List<DrawnItem> draws, int coinsGained, int coins, int packsSinceRare) {}
