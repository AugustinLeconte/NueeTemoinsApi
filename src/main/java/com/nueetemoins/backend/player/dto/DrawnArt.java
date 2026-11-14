package com.nueetemoins.backend.player.dto;

import com.nueetemoins.backend.card.dto.CardArtResponse;

public record DrawnArt(CardArtResponse art, boolean newArt, boolean newCard, int fragmentsGained) {}
