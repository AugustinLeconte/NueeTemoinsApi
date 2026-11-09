package com.nueetemoins.backend.card.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeastDateRequest(
    @NotBlank @Size(min = 2, max = 2) String countryCode,
    @Min(1) @Max(12) int month,
    @Min(1) @Max(31) int day
) {}