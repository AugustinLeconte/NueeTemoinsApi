package com.nueetemoins.backend.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nueetemoins.backend.achievement.AchievementService;
import com.nueetemoins.backend.achievement.dto.AchievementResponse;
import com.nueetemoins.backend.player.dto.PackOfferResponse;
import com.nueetemoins.backend.player.dto.PackOfferStatus;
import com.nueetemoins.backend.player.dto.PackResult;
import com.nueetemoins.backend.player.dto.ProgressResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlayerProgressService {

    private static final int PREVIEW_LEVELS = 3;

    private final PlayerCollectionService collection;
    private final PlayerPackClaimRepository claimRepository;
    private final PackService packService;
    private final AchievementService achievementService;

    @Transactional
    public ProgressResponse progress(String userId) {
        return collection.progressOf(collection.lockWallet(userId));
    }

    @Transactional
    public ProgressResponse awardXp(String userId, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        PlayerWallet wallet = collection.lockWallet(userId);
        collection.addXp(wallet, amount);
        achievementService.checkAndUnlock(userId, wallet);
        return collection.progressOf(wallet);
    }

    @Transactional
    public List<AchievementResponse> achievements(String userId) {
        PlayerWallet wallet = collection.lockWallet(userId);
        return achievementService.list(userId, wallet);
    }

    @Transactional
    public List<PackOfferResponse> packOffers(String userId) {
        PlayerWallet wallet = collection.lockWallet(userId);
        int level = GameRules.levelForXp(wallet.getXp());
        Set<Integer> opened = claimRepository.findOpenedLevelsByUserId(userId);
        Set<Integer> stored = claimRepository.findStoredLevelsByUserId(userId);

        List<PackOfferResponse> offers = new ArrayList<>();
        int last = Math.min(GameRules.MAX_PLAYER_LEVEL, level + PREVIEW_LEVELS);
        for (int l = 2; l <= last; l++) {
            PackType type = GameRules.levelPackType(l);
            PackOfferStatus status =
                opened.contains(l) ? PackOfferStatus.OPENED
                : stored.contains(l) ? PackOfferStatus.CLAIMED
                : l <= level         ? PackOfferStatus.AVAILABLE
                :                      PackOfferStatus.LOCKED;
            offers.add(new PackOfferResponse(l, type.name(), type.size, status));
        }
        return offers;
    }

    /** Boutique : met le pack de niveau en stock (sans l'ouvrir). */
    @Transactional
    public List<PackOfferResponse> claimLevelPack(String userId, int level) {
        PlayerWallet wallet = collection.lockWallet(userId);
        int playerLevel = GameRules.levelForXp(wallet.getXp());
        if (level < 2 || level > playerLevel) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Pack not unlocked");
        }
        if (claimRepository.existsByUserIdAndLevel(userId, level)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pack already claimed");
        }
        PlayerPackClaim claim = new PlayerPackClaim();
        claim.setUserId(userId);
        claim.setLevel(level);
        claim.setOpened(false);
        claimRepository.save(claim);
        return packOffers(userId);
    }

    /** Page Packs : ouvre un pack précédemment récupéré. */
    @Transactional
    public PackResult openLevelPack(String userId, int level, String lang, String country) {
        PlayerWallet wallet = collection.lockWallet(userId);
        PlayerPackClaim claim = claimRepository.findByUserIdAndLevel(userId, level)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Pack not claimed"));
        if (claim.isOpened()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pack already opened");
        }
        claim.setOpened(true);
        return packService.open(userId, wallet, GameRules.levelPackType(level), lang, country);
    }
}
