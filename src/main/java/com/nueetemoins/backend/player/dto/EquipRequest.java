package com.nueetemoins.backend.player.dto;

import jakarta.validation.constraints.NotBlank;

public record EquipRequest(@NotBlank String artPublicId) {}
