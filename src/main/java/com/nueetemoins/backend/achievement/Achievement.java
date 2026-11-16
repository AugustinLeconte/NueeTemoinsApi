package com.nueetemoins.backend.achievement;

import static com.nueetemoins.backend.achievement.AchievementMetric.*;


enum Achievement {
    LEVEL_1(PLAYER_LEVEL, 1),
    LEVEL_10(PLAYER_LEVEL, 10),
    LEVEL_25(PLAYER_LEVEL, 25),
    LEVEL_50(PLAYER_LEVEL, 50),

    CARDS_10(CARDS_OWNED, 10),
    ARTS_5_ON_ONE_CARD(ARTS_ON_ONE_CARD, 5),

    COINS_100(COINS, 100),

    SHOP_FIRST_PURCHASE(SHOP_PURCHASES, 1),
    SHOP_20_PURCHASES(SHOP_PURCHASES, 20);

    final AchievementMetric metric;
    final int target;

    Achievement(AchievementMetric metric, int target) {
        this.metric = metric;
        this.target = target;
    }
}