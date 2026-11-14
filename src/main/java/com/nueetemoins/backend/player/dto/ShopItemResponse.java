package com.nueetemoins.backend.player.dto;

import com.nueetemoins.backend.card.dto.CardArtResponse;

public record ShopItemResponse(CardArtResponse art, int price, boolean owned) {}
