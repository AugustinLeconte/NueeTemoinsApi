package com.nueetemoins.backend.player.dto;

/** Pack débloqué par le passage au niveau `level`. */
public record PackOfferResponse(int level, String packType, int size, PackOfferStatus status) {}
