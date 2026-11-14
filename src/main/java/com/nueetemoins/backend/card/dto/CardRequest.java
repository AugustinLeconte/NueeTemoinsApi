package com.nueetemoins.backend.card.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CardRequest(
    @NotEmpty List<String> families,
    @NotEmpty @Valid List<TranslationRequest> translations,
    @Valid List<FeastDateRequest> feastDates
) {}
