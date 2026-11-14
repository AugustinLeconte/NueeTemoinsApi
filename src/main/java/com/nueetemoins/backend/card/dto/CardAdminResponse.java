package com.nueetemoins.backend.card.dto;

import java.util.List;

public record CardAdminResponse(
    String publicId, List<String> families,
    List<TranslationRequest> translations, List<FeastDateRequest> feastDates
) {}
