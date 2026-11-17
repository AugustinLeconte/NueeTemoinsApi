package com.nueetemoins.backend.achievement;

import static com.nueetemoins.backend.achievement.AchievementMetric.*;


enum Achievement {
    LEVEL_2(PLAYER_LEVEL, 2),
    LEVEL_10(PLAYER_LEVEL, 10),
    LEVEL_25(PLAYER_LEVEL, 25),
    LEVEL_50(PLAYER_LEVEL, 50),

    CARDS_10(CARDS_OWNED, 10),
    ARTS_5_ON_ONE_CARD(ARTS_ON_ONE_CARD, 5),

    COINS_100(COINS, 100),

    SHOP_FIRST_PURCHASE(SHOP_PURCHASES, 1),
    SHOP_20_PURCHASES(SHOP_PURCHASES, 20),

    MINIGAME_FIRST_WIN(MINIGAME_WINS, 1),
    MINIGAME_10_WINS(MINIGAME_WINS, 10),
    MINIGAME_50_WINS(MINIGAME_WINS, 50),
    MINIGAME_PERFECT(MINIGAME_PERFECT_WINS, 1),
    MINIGAME_STREAK_3(MINIGAME_STREAK, 3),
    MINIGAME_STREAK_7(MINIGAME_STREAK, 7),
    MINIGAME_STREAK_30(MINIGAME_STREAK, 30),
    MINIGAME_3_GAMES(MINIGAME_DISTINCT_GAMES_WON, 3);

    final AchievementMetric metric;
    final int target;

    Achievement(AchievementMetric metric, int target) {
        this.metric = metric;
        this.target = target;
    }
}