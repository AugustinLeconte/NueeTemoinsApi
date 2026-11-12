package com.nueetemoins.backend.card.dto;

import java.util.List;

import com.nueetemoins.backend.card.CardRarity;
import com.nueetemoins.backend.family.dto.FamilyResponse;

public record CardResponse(
    String publicId, List<FamilyResponse> families, CardRarity rarity, String imageUrl,
    String locale, String title, String subtitle, String description,
    String countryCode, Integer feastMonth, Integer feastDay
) {}