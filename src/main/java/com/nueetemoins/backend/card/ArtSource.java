package com.nueetemoins.backend.card;

/** Comment une illustration peut être obtenue. */
public enum ArtSource {
    DEFAULT, // illustration de base, donnée avec le saint
    PACK,    // tirable dans les packs
    SHOP,    // achetable en boutique (pièces)
    EVENT    // distribuée manuellement / lors d'événements
}
