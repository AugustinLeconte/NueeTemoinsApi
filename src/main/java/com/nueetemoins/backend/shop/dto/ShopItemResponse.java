package com.nueetemoins.backend.shop.dto;

import com.nueetemoins.backend.card.dto.CardArtResponse;

public record ShopItemResponse(CardArtResponse art, int price, boolean owned) {}
