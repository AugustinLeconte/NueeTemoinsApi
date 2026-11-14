package com.nueetemoins.backend.player.dto;

import java.util.List;

public record PackResult(List<DrawnArt> draws, int fragmentsGained, int fragments, int packsSinceRare) {}
