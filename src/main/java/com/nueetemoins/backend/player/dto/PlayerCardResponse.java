package com.nueetemoins.backend.player.dto;

import java.util.List;

import com.nueetemoins.backend.card.dto.CardArtResponse;
import com.nueetemoins.backend.card.dto.CardResponse;

public record PlayerCardResponse(
    CardResponse card,
    int level,
    CardArtResponse equippedArt,
    List<CardArtResponse> ownedArts,
    Integer nextUpgradeCost // null si niveau max
) {}
