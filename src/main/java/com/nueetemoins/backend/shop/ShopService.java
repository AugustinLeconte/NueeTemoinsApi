package com.nueetemoins.backend.shop;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nueetemoins.backend.achievement.AchievementService;
import com.nueetemoins.backend.card.ArtSource;
import com.nueetemoins.backend.card.CardArt;
import com.nueetemoins.backend.card.CardArtRepository;
import com.nueetemoins.backend.card.CardArtService;
import com.nueetemoins.backend.player.GameRules;
import com.nueetemoins.backend.player.PlayerArtRepository;
import com.nueetemoins.backend.player.PlayerCollectionService;
import com.nueetemoins.backend.player.PlayerWallet;
import com.nueetemoins.backend.player.dto.WalletResponse;
import com.nueetemoins.backend.shop.dto.ShopItemResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final CardArtRepository cardArtRepository;
    private final PlayerArtRepository playerArtRepository;
    private final PlayerCollectionService collection;
    private final CardArtService cardArtService;
    private final AchievementService achievementService;

    /** Même joueur + même jour = mêmes illustrations. */
    private List<CardArt> dailyShopArts(String userId) {
        List<CardArt> pool = new ArrayList<>(cardArtRepository.findBySourceWithCard(ArtSource.SHOP));
        pool.addAll(cardArtRepository.findBySourceWithCard(ArtSource.PACK));
        pool.removeIf(a -> !a.isObtainable());
        pool.sort(Comparator.comparing(CardArt::getPublicId));
        long seed = LocalDate.now(PlayerCollectionService.ZONE).toEpochDay() * 31L + userId.hashCode();
        Collections.shuffle(pool, new Random(seed));
        return pool.stream().limit(GameRules.SHOP_ART_COUNT).toList();
    }

    @Transactional(readOnly = true)
    public List<ShopItemResponse> shop(String userId, String lang) {
        Set<UUID> owned = playerArtRepository.findOwnedArtIds(userId);
        return dailyShopArts(userId).stream()
            .map(a -> new ShopItemResponse(
                cardArtService.toResponse(a, lang),
                GameRules.priceOf(a.getRarity()),
                owned.contains(a.getId())))
            .toList();
    }

    @Transactional
    public WalletResponse buy(String userId, String artPublicId) {
        CardArt art = dailyShopArts(userId).stream()
            .filter(a -> a.getPublicId().equals(artPublicId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not in today's offer"));

        PlayerWallet wallet = collection.lockWallet(userId);
        if (playerArtRepository.existsByUserIdAndArtId(userId, art.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Art already owned");
        }
        int price = GameRules.priceOf(art.getRarity());
        if (wallet.getCoins() < price) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough coins");
        }
        wallet.setCoins(wallet.getCoins() - price);
        collection.grantArt(userId, art);
        List<String> newAch = achievementService.checkAndUnlock(userId, wallet);
        return collection.toResponse(wallet, newAch);
    }
}