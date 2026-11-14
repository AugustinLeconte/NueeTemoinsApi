package com.nueetemoins.backend.player;

import com.nueetemoins.backend.card.CardRarity;

/** Constantes d'équilibrage centralisées. À terme : application.yml / table de config. */
final class GameRules {
    private GameRules() {}

    static final int MAX_LEVEL = 5;
    /** Après N packs sans RARE+, le prochain pack en garantit un. */
    static final int PITY_THRESHOLD = 10;
    static final CardRarity PITY_MIN = CardRarity.RARE;
    /** Multiplicateur de probabilité du saint fêté le jour même. */
    static final int FEAST_BOOST = 5;

    /** Fragments gagnés quand on tire une illustration déjà possédée. */
    static int duplicateFragments(CardRarity r) {
        return switch (r) {
            case COMMON -> 5;
            case UNCOMMON -> 10;
            case RARE -> 25;
            case EPIC -> 60;
            case LEGENDARY -> 150;
        };
    }

    /** Coût pour passer de `level` à `level + 1`. */
    static int upgradeCost(int level) { return level * 50; }
}
