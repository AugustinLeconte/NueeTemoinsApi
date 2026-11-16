package com.nueetemoins.backend.player;

/** Résultat de l'attribution d'une illustration (état du saint APRÈS attribution). */
record GrantResult(boolean newArt, boolean newCard, int fragmentsGained,
                   int cardFragments, int cardLevel, Integer nextUpgradeCost) {}
