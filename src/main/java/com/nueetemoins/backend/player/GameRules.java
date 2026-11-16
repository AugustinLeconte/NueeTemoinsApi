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

    // ---------- Niveaux du joueur ----------
    static final int MAX_PLAYER_LEVEL = 50;

    // XP gagnée par action
    static final int XP_PACK_OPENED = 25;
    static final int XP_NEW_CARD = 15;      // nouveau saint dans un pack
    static final int XP_NEW_ART = 5;        // nouvelle illustration d'un saint déjà possédé
    static final int XP_CARD_UPGRADE = 20;  // montée de niveau d'un saint

    /** XP total nécessaire pour atteindre le niveau L : 100, 250, 450, 700... (paliers de +100, +150, +200...). */
    static int xpToReach(int level) {
        int n = level - 1;
        return 100 * n + 25 * n * (n - 1);
    }

    static int levelForXp(int xp) {
        int level = 1;
        while (level < MAX_PLAYER_LEVEL && xp >= xpToReach(level + 1)) level++;
        return level;
    }

    /** Pack débloqué à chaque niveau (niveaux 2+) : premium tous les 5 niveaux. */
    static PackType levelPackType(int level) {
        return level % 5 == 0 ? PackType.PREMIUM : PackType.STANDARD;
    }

    /** Fragments du saint gagnés quand on tire une de ses illustrations déjà possédée. */
    static int duplicateFragments(CardRarity r) {
        return switch (r) {
            case COMMON -> 5;
            case UNCOMMON -> 10;
            case RARE -> 25;
            case EPIC -> 60;
            case LEGENDARY -> 150;
        };
    }

    /** Coût (en fragments du saint) pour passer du niveau n au niveau n+1 : index = n - 1. */
    private static final int[] UPGRADE_COSTS = {20, 50, 100, 200};

    /** Coût du prochain niveau, ou null si le niveau maximum est atteint. */
    static Integer nextUpgradeCost(int level) {
        return level >= MAX_LEVEL ? null : UPGRADE_COSTS[level - 1];
    }
}
