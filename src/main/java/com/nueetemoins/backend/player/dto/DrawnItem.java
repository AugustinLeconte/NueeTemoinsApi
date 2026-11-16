package com.nueetemoins.backend.player.dto;

import com.nueetemoins.backend.card.dto.CardArtResponse;

/** Un emplacement du pack : soit une illustration (ART), soit des pièces (COINS). */
public record DrawnItem(
    DrawnType type,
    CardArtResponse art,        // ART uniquement
    boolean newArt,
    boolean newCard,
    int fragmentsGained,        // doublon : fragments ajoutés au saint
    Integer cardFragments,      // fragments du saint après tirage
    Integer cardLevel,
    Integer nextUpgradeCost,    // null si niveau max
    int coinsGained             // COINS uniquement
) {
    public static DrawnItem ofArt(CardArtResponse art, boolean newArt, boolean newCard, int fragmentsGained,
                                  int cardFragments, int cardLevel, Integer nextUpgradeCost) {
        return new DrawnItem(DrawnType.ART, art, newArt, newCard, fragmentsGained,
            cardFragments, cardLevel, nextUpgradeCost, 0);
    }

    public static DrawnItem ofCoins(int amount) {
        return new DrawnItem(DrawnType.COINS, null, false, false, 0, null, null, null, amount);
    }
}
