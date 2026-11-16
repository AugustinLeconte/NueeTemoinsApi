package com.nueetemoins.backend.achievement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nueetemoins.backend.card.ArtSource;
import com.nueetemoins.backend.player.GameRules;
import com.nueetemoins.backend.player.PlayerArtRepository;
import com.nueetemoins.backend.player.PlayerCardRepository;
import com.nueetemoins.backend.player.PlayerWallet;
import com.nueetemoins.backend.achievement.dto.AchievementResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final PlayerAchievementRepository achievementRepository;
    private final PlayerCardRepository playerCardRepository;
    private final PlayerArtRepository playerArtRepository;

    @Transactional
    public List<String> checkAndUnlock(String userId, PlayerWallet wallet) {
        Set<String> unlocked = achievementRepository.findCodesByUserId(userId);
        Map<AchievementMetric, Integer> cache = new EnumMap<>(AchievementMetric.class);
        List<String> newly = new ArrayList<>();

        for (Achievement a : Achievement.values()) {
            if (unlocked.contains(a.name())) continue;
            int value = cache.computeIfAbsent(a.metric, m -> metricValue(m, userId, wallet));
            if (value >= a.target) {
                PlayerAchievement pa = new PlayerAchievement();
                pa.setUserId(userId);
                pa.setCode(a.name());
                achievementRepository.save(pa);
                newly.add(a.name());
            }
        }
        return newly;
    }

    /** Liste complète (débloqués + progression des autres), pour l'écran des succès. */
    @SuppressWarnings("null")
    @Transactional
    public List<AchievementResponse> list(String userId, PlayerWallet wallet) {
        checkAndUnlock(userId, wallet); // rattrape aussi les joueurs existants
        Map<String, Instant> unlocked = achievementRepository.findByUserId(userId).stream()
            .collect(Collectors.toMap(PlayerAchievement::getCode, PlayerAchievement::getUnlockedAt));
        Map<AchievementMetric, Integer> cache = new EnumMap<>(AchievementMetric.class);

        List<AchievementResponse> out = new ArrayList<>();
        for (Achievement a : Achievement.values()) {
            Instant at = unlocked.get(a.name());
            int progress = at != null
                ? a.target
                : Math.min(a.target, cache.computeIfAbsent(a.metric, m -> metricValue(m, userId, wallet)));
            out.add(new AchievementResponse(a.name(), at != null, at, progress, a.target));
        }
        return out;
    }

    @SuppressWarnings("null")
    private int metricValue(AchievementMetric metric, String userId, PlayerWallet wallet) {
        return switch (metric) {
            case PLAYER_LEVEL -> GameRules.levelForXp(wallet.getXp());
            case CARDS_OWNED -> (int) playerCardRepository.countByUserId(userId);
            case ARTS_ON_ONE_CARD -> playerArtRepository.countArtsPerCard(userId).stream()
                .mapToInt(Long::intValue).max().orElse(0);
            case COINS -> wallet.getCoins();
            case SHOP_PURCHASES -> (int) playerArtRepository.countByUserIdAndArtSource(userId, ArtSource.SHOP);
        };
    }
}