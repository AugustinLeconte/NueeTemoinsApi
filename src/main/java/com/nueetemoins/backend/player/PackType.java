package com.nueetemoins.backend.player;

import com.nueetemoins.backend.card.CardRarity;

/** Taille du pack + poids par rareté (ordre : COMMON, UNCOMMON, RARE, EPIC, LEGENDARY). */
public enum PackType {
    STANDARD(3, new int[] {700, 200, 80, 18, 2}),
    PREMIUM(5, new int[] {450, 300, 170, 65, 15});

    final int size;
    private final int[] weights;

    PackType(int size, int[] weights) {
        this.size = size;
        this.weights = weights;
    }

    int weightOf(CardRarity r) { return weights[r.ordinal()]; }
}
