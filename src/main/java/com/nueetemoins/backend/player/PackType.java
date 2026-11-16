package com.nueetemoins.backend.player;

import com.nueetemoins.backend.card.CardRarity;

/**
 * Taille du pack, poids par rareté (ordre : COMMON, UNCOMMON, RARE, EPIC, LEGENDARY)
 * et paramètres des emplacements "pièces".
 */
public enum PackType {
    //        taille  poids par rareté                  % pièces  min  max
    STANDARD(3, new int[] {700, 200, 80, 18, 2},        15,       20,  60),
    PREMIUM (5, new int[] {450, 300, 170, 65, 15},      15,       50, 150);

    final int size;
    private final int[] weights;
    final int coinChancePercent;
    final int coinMin;
    final int coinMax;

    PackType(int size, int[] weights, int coinChancePercent, int coinMin, int coinMax) {
        this.size = size;
        this.weights = weights;
        this.coinChancePercent = coinChancePercent;
        this.coinMin = coinMin;
        this.coinMax = coinMax;
    }

    int weightOf(CardRarity r) { return weights[r.ordinal()]; }
}
