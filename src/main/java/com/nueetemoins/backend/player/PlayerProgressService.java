package com.nueetemoins.backend.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nueetemoins.backend.player.dto.PackOfferResponse;
import com.nueetemoins.backend.player.dto.PackOfferStatus;
import com.nueetemoins.backend.player.dto.PackResult;
import com.nueetemoins.backend.player.dto.ProgressResponse;

import lombok.RequiredArgsConstructor;

/** Niveau / XP du joueur et packs débloqués par les passages de niveau. */
@Service
@RequiredArgsConstructor
public class PlayerProgressService {

    /** Nombre de packs verrouillés affichés à l'avance dans la boutique. */
    private static final int PREVIEW_LEVELS = 3;

    private final PlayerCollectionService collection;
    private final PlayerPackClaimRepository claimRepository;
    private final PackService packService;

    @Transactional
    public ProgressResponse progress(String userId) {
        return collection.progressOf(collection.lockWallet(userId));
    }

    /**
     * Point d'entrée pour les futurs jeux / activités : donne de l'XP à un joueur.
     * À appeler UNIQUEMENT depuis le serveur, jamais avec un montant fourni par le client.
     */
    @Transactional
    public ProgressResponse awardXp(String userId, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        PlayerWallet wallet = collection.lockWallet(userId);
        collection.addXp(wallet, amount);
        return collection.progressOf(wallet);
    }

    @Transactional
    public List<PackOfferResponse> packOffers(String userId) {
        PlayerWallet wallet = collection.lockWallet(userId);
        int level = GameRules.levelForXp(wallet.getXp());
        Set<Integer> opened = claimRepository.findLevelsByUserId(userId);

        List<PackOfferResponse> offers = new ArrayList<>();
        int last = Math.min(GameRules.MAX_PLAYER_LEVEL, level + PREVIEW_LEVELS);
        for (int l = 2; l <= last; l++) {
            PackType type = GameRules.levelPackType(l);
            PackOfferStatus status = opened.contains(l) ? PackOfferStatus.OPENED
                : l <= level ? PackOfferStatus.AVAILABLE : PackOfferStatus.LOCKED;
            offers.add(new PackOfferResponse(l, type.name(), type.size, status));
        }
        return offers;
    }

    /** Ouvre le pack débloqué au niveau `level` (une seule fois par joueur et par niveau). */
    @Transactional
    public PackResult openLevelPack(String userId, int level, String lang, String country) {
        PlayerWallet wallet = collection.lockWallet(userId);
        int playerLevel = GameRules.levelForXp(wallet.getXp());
        if (level < 2 || level > playerLevel) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Pack not unlocked");
        }
        if (claimRepository.existsByUserIdAndLevel(userId, level)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pack already opened");
        }
        PlayerPackClaim claim = new PlayerPackClaim();
        claim.setUserId(userId);
        claim.setLevel(level);
        claimRepository.save(claim);

        return packService.open(userId, wallet, GameRules.levelPackType(level), lang, country);
    }
}
