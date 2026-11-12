package com.nueetemoins.backend.card.dto;

import java.util.List;

import com.nueetemoins.backend.card.CardRarity;

public record CardAdminResponse(
    String publicId, List<String> families, CardRarity rarity, String imageUrl,
    List<TranslationRequest> translations, List<FeastDateRequest> feastDates
) {}