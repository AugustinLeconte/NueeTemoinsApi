package com.nueetemoins.backend.player.dto;

/**
 * @param xp             XP total
 * @param xpIntoLevel    XP accumulée dans le niveau en cours
 * @param xpForNextLevel XP à accumuler dans ce niveau pour passer au suivant (null au niveau max)
 * @param unclaimedPacks packs de niveau débloqués mais pas encore ouverts en boutique
 */
public record ProgressResponse(int level, int xp, int xpIntoLevel, Integer xpForNextLevel, int unclaimedPacks) {}
