package com.nueetemoins.backend.shop.dto;

public record FreeOffersResponse(int dailyCoins, boolean dailyAvailable,
                                 int adCoins, int adRemaining, int adMax) {}