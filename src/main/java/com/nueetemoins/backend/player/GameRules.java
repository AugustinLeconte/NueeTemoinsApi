package com.nueetemoins.backend.player;

import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;

import com.nueetemoins.backend.card.CardRarity;

public final class GameRules {
    private GameRules() {}

    public static final ZoneId GAME_ZONE = ZoneId.of("Europe/Paris");
    public static final int XP_MINIGAME_MIN = 5;

    private static final int[] WORDLE_WIN_XP = {50, 30, 25, 20, 15, 10};

    public static final int MAX_LEVEL = 5;
    public static final int PITY_THRESHOLD = 10;
    public static final CardRarity PITY_MIN = CardRarity.RARE;
    public static final int FEAST_BOOST = 5;

    public static final int MAX_PLAYER_LEVEL = 50;

    public static final int XP_PACK_OPENED = 25;
    public static final int XP_NEW_CARD = 15;
    public static final int XP_NEW_ART = 5;
    public static final int XP_CARD_UPGRADE = 20;

    public static final int DAILY_COINS = 25;
    public static final int AD_COINS = 50;
    public static final int AD_MAX_PER_DAY = 5;
    public static final int AD_COOLDOWN_SECONDS = 60;
    public static final int SHOP_ART_COUNT = 3;

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

    private static final Map<CardRarity, Integer> ART_PRICES = new EnumMap<>(Map.of(
        CardRarity.COMMON, 100,
        CardRarity.UNCOMMON, 200,
        CardRarity.RARE, 300,
        CardRarity.EPIC, 500,
        CardRarity.LEGENDARY, 750
    ));

    public static int priceOf(CardRarity r) {
        return ART_PRICES.getOrDefault(r, 100);
    }

    static Integer nextUpgradeCost(int level) {
        return level >= MAX_LEVEL ? null : UPGRADE_COSTS[level - 1];
    }

    public static int wordleXp(boolean won, int attempts) {
        if (!won) return XP_MINIGAME_MIN;
        int i = Math.min(Math.max(attempts, 1), WORDLE_WIN_XP.length) - 1;
        return Math.max(XP_MINIGAME_MIN, WORDLE_WIN_XP[i]);
    }
}
