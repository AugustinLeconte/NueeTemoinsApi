package com.nueetemoins.backend.player;

import com.nueetemoins.backend.card.CardRarity;

public final class GameRules {
    private GameRules() {}

    static final int MAX_LEVEL = 5;
    static final int PITY_THRESHOLD = 10;
    static final CardRarity PITY_MIN = CardRarity.RARE;
    static final int FEAST_BOOST = 5;

    static final int MAX_PLAYER_LEVEL = 50;

    static final int XP_PACK_OPENED = 25;
    static final int XP_NEW_CARD = 15;
    static final int XP_NEW_ART = 5;
    static final int XP_CARD_UPGRADE = 20;

    static int xpToReach(int level) {
        int n = level - 1;
        return 100 * n + 25 * n * (n - 1);
    }

    public static int levelForXp(int xp) {
        int level = 1;
        while (level < MAX_PLAYER_LEVEL && xp >= xpToReach(level + 1)) level++;
        return level;
    }

    static PackType levelPackType(int level) {
        return level % 5 == 0 ? PackType.PREMIUM : PackType.STANDARD;
    }

    static int duplicateFragments(CardRarity r) {
        return switch (r) {
            case COMMON -> 5;
            case UNCOMMON -> 10;
            case RARE -> 25;
            case EPIC -> 60;
            case LEGENDARY -> 150;
        };
    }

    private static final int[] UPGRADE_COSTS = {20, 50, 100, 200};

    static Integer nextUpgradeCost(int level) {
        return level >= MAX_LEVEL ? null : UPGRADE_COSTS[level - 1];
    }
}
