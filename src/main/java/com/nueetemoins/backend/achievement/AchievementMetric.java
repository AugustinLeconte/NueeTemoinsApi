package com.nueetemoins.backend.achievement;

enum AchievementMetric {
    PLAYER_LEVEL,       // niveau du joueur
    CARDS_OWNED,        // nombre de saints possédés
    ARTS_ON_ONE_CARD,   // max d'illustrations possédées sur un même saint
    COINS,              // solde de pièces actuel
    SHOP_PURCHASES,     // illustrations achetées en boutique,
    MINIGAME_WINS,            // parties gagnées, tous jeux confondus
    MINIGAME_PERFECT_WINS,    // victoires "parfaites" (ex. Wordle du 1er coup)
    MINIGAME_STREAK,          // jours consécutifs avec au moins une partie terminée
    MINIGAME_DISTINCT_GAMES_WON // nb de mini-jeux différents déjà gagnés
}
