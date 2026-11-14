package com.nueetemoins.backend.card.dto;

import jakarta.validation.constraints.NotBlank;

public record ArtistRequest(@NotBlank String name, String bio, String url) {}
