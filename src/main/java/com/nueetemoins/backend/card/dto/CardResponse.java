package com.nueetemoins.backend.card.dto;

import java.util.List;

import com.nueetemoins.backend.family.dto.FamilyResponse;

public record CardResponse(
    String publicId,
    List<FamilyResponse> families,
    String locale, String title, String subtitle, String description,
    String feastCountryCode, Integer feastMonth, Integer feastDay,
    CardArtResponse defaultArt
) {}
