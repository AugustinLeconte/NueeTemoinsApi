package com.nueetemoins.backend.shop;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.nueetemoins.backend.player.GameRules;
import com.nueetemoins.backend.player.PlayerCollectionService;
import com.nueetemoins.backend.player.PlayerWallet;
import com.nueetemoins.backend.player.dto.WalletResponse;
import com.nueetemoins.backend.shop.dto.FreeOffersResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShopRewardService {

    private final PlayerCollectionService collection;

    @Transactional
    public FreeOffersResponse offers(String userId) {
        PlayerWallet w = collection.lockWallet(userId);
        LocalDate today = LocalDate.now(PlayerCollectionService.ZONE);
        int adsDone = today.equals(w.getAdRewardsDate()) ? w.getAdRewardsCount() : 0;
        return new FreeOffersResponse(
            GameRules.DAILY_COINS, !today.equals(w.getLastDailyCoins()),
            GameRules.AD_COINS, Math.max(0, GameRules.AD_MAX_PER_DAY - adsDone), GameRules.AD_MAX_PER_DAY);
    }

    @Transactional
    public WalletResponse claimDaily(String userId) {
        PlayerWallet w = collection.lockWallet(userId);
        LocalDate today = LocalDate.now(PlayerCollectionService.ZONE);
        if (today.equals(w.getLastDailyCoins())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Daily coins already claimed");
        }
        w.setLastDailyCoins(today);
        w.setCoins(w.getCoins() + GameRules.DAILY_COINS);
        return collection.wallet(userId);
    }

    @Transactional
    public WalletResponse claimAd(String userId) {
        PlayerWallet w = collection.lockWallet(userId);
        LocalDate today = LocalDate.now(PlayerCollectionService.ZONE);
        if (!today.equals(w.getAdRewardsDate())) {
            w.setAdRewardsDate(today);
            w.setAdRewardsCount(0);
        }
        if (w.getAdRewardsCount() >= GameRules.AD_MAX_PER_DAY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Daily ad limit reached");
        }
        Instant last = w.getLastAdRewardAt();
        if (last != null && last.plusSeconds(GameRules.AD_COOLDOWN_SECONDS).isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too soon");
        }
        w.setAdRewardsCount(w.getAdRewardsCount() + 1);
        w.setLastAdRewardAt(Instant.now());
        w.setCoins(w.getCoins() + GameRules.AD_COINS);
        return collection.wallet(userId);
    }
}