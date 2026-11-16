package com.nueetemoins.backend.player;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nueetemoins.backend.card.ArtSource;
import com.nueetemoins.backend.card.CardArt;
import com.nueetemoins.backend.card.CardArtRepository;
import com.nueetemoins.backend.card.CardArtService;
import com.nueetemoins.backend.card.CardService;
import com.nueetemoins.backend.player.dto.PlayerCardResponse;
import com.nueetemoins.backend.player.dto.ProgressResponse;
import com.nueetemoins.backend.player.dto.ShopItemResponse;
import com.nueetemoins.backend.player.dto.UpgradeResponse;
import com.nueetemoins.backend.player.dto.WalletResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlayerCollectionService {

    static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private final PlayerWalletRepository walletRepository;
    private final PlayerCardRepository playerCardRepository;
    private final PlayerArtRepository playerArtRepository;
    private final CardArtRepository cardArtRepository;
    private final CardService cardService;
    private final CardArtService cardArtService;
    private final PlayerPackClaimRepository claimRepository;

    // ---------- Wallet ----------

    /**
     * Récupère (ou crée) le wallet avec un verrou en écriture.
     * Sert aussi de verrou global du joueur : toute action qui modifie ses fragments,
     * niveaux ou pièces commence par là, ce qui évite les accès concurrents.
     */
    @Transactional
    public PlayerWallet lockWallet(String userId) {
        return walletRepository.findForUpdate(userId).orElseGet(() -> {
            PlayerWallet w = new PlayerWallet();
            w.setUserId(userId);
            return walletRepository.saveAndFlush(w);
        });
    }

    @Transactional
    public WalletResponse wallet(String userId) {
        return toResponse(lockWallet(userId));
    }

    WalletResponse toResponse(PlayerWallet w) {
        boolean available = !LocalDate.now(ZONE).equals(w.getLastDailyPack());
        return new WalletResponse(w.getCoins(), w.getPacksSinceRare(), available);
    }

    // ---------- Progression du joueur ----------

    /** Ajoute de l'XP (plafonnée au niveau max). Renvoie true si le joueur change de niveau. */
    boolean addXp(PlayerWallet w, int amount) {
        int before = GameRules.levelForXp(w.getXp());
        w.setXp(Math.min(w.getXp() + amount, GameRules.xpToReach(GameRules.MAX_PLAYER_LEVEL)));
        return GameRules.levelForXp(w.getXp()) > before;
    }

    ProgressResponse progressOf(PlayerWallet w) {
        int xp = w.getXp();
        int level = GameRules.levelForXp(xp);
        int base = GameRules.xpToReach(level);
        Integer span = level >= GameRules.MAX_PLAYER_LEVEL ? null : GameRules.xpToReach(level + 1) - base;
        // un pack par niveau à partir du niveau 2
        int unclaimed = Math.max(0, (level - 1) - (int) claimRepository.countByUserId(w.getUserId()));
        return new ProgressResponse(level, xp, xp - base, span, unclaimed);
    }

    // ---------- Collection ----------

    @Transactional(readOnly = true)
    public List<PlayerCardResponse> collection(String userId, String lang, String country) {
        List<PlayerCard> cards = playerCardRepository.findByUserId(userId);
        Map<UUID, List<CardArt>> owned = playerArtRepository.findByUserId(userId).stream()
            .map(PlayerArt::getArt)
            .collect(Collectors.groupingBy(a -> a.getCard().getId()));

        return cards.stream().map(pc -> new PlayerCardResponse(
            cardService.toResponse(pc.getCard(), lang, country),
            pc.getLevel(),
            pc.getFragments(),
            cardArtService.toResponse(pc.getEquippedArt(), lang),
            owned.getOrDefault(pc.getCard().getId(), List.of()).stream()
                .map(a -> cardArtService.toResponse(a, lang)).toList(),
            GameRules.nextUpgradeCost(pc.getLevel())
        )).toList();
    }

    @Transactional
    public void equip(String userId, String cardPublicId, String artPublicId) {
        PlayerCard pc = getPlayerCard(userId, cardPublicId);
        CardArt art = cardArtRepository.findByPublicId(artPublicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Art not found"));
        if (!art.getCard().getId().equals(pc.getCard().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Art does not belong to this card");
        }
        if (!playerArtRepository.existsByUserIdAndArtId(userId, art.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Art not owned");
        }
        pc.setEquippedArt(art);
    }

    /** Monte un saint d'un niveau en dépensant SES fragments. */
    @Transactional
    public UpgradeResponse upgrade(String userId, String cardPublicId) {
        PlayerWallet wallet = lockWallet(userId); // verrou global du joueur
        PlayerCard pc = getPlayerCard(userId, cardPublicId);
        Integer cost = GameRules.nextUpgradeCost(pc.getLevel());
        if (cost == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Max level reached");
        }
        if (pc.getFragments() < cost) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough fragments");
        }
        pc.setFragments(pc.getFragments() - cost);
        pc.setLevel(pc.getLevel() + 1);
        boolean playerLevelUp = addXp(wallet, GameRules.XP_CARD_UPGRADE);
        return new UpgradeResponse(pc.getLevel(), pc.getFragments(), GameRules.nextUpgradeCost(pc.getLevel()),
            GameRules.XP_CARD_UPGRADE, playerLevelUp, progressOf(wallet));
    }

    // ---------- Boutique (pièces) ----------

    @Transactional(readOnly = true)
    public List<ShopItemResponse> shop(String userId, String lang) {
        Set<UUID> owned = playerArtRepository.findOwnedArtIds(userId);
        return cardArtRepository.findBySourceWithCard(ArtSource.SHOP).stream()
            .filter(a -> a.getCoinPrice() != null)
            .map(a -> new ShopItemResponse(
                cardArtService.toResponse(a, lang), a.getCoinPrice(), owned.contains(a.getId())))
            .toList();
    }

    @Transactional
    public WalletResponse buy(String userId, String artPublicId) {
        CardArt art = cardArtRepository.findByPublicId(artPublicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Art not found"));
        if (art.getSource() != ArtSource.SHOP || !art.isObtainable() || art.getCoinPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Art is not for sale");
        }
        PlayerWallet wallet = lockWallet(userId);
        if (playerArtRepository.existsByUserIdAndArtId(userId, art.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Art already owned");
        }
        if (wallet.getCoins() < art.getCoinPrice()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough coins");
        }
        wallet.setCoins(wallet.getCoins() - art.getCoinPrice());
        grantArt(userId, art);
        return toResponse(wallet);
    }

    // ---------- Attribution ----------

    /**
     * Donne une illustration au joueur.
     * - déjà possédée (doublon) : les fragments vont sur le saint concerné
     * - nouveau saint : création du PlayerCard + illustration par défaut offerte
     * Le retour décrit l'état du saint après attribution.
     */
    @Transactional
    public GrantResult grantArt(String userId, CardArt art) {
        UUID cardId = art.getCard().getId();
        boolean ownsArt = playerArtRepository.existsByUserIdAndArtId(userId, art.getId());

        Optional<PlayerCard> existing = playerCardRepository.findByUserIdAndCardId(userId, cardId);
        boolean newCard = existing.isEmpty();
        PlayerCard pc = existing.orElseGet(() -> createPlayerCard(userId, art));

        if (ownsArt) {
            int fragments = GameRules.duplicateFragments(art.getRarity());
            pc.setFragments(pc.getFragments() + fragments);
            return result(pc, false, newCard, fragments);
        }

        saveOwnedArt(userId, art);
        if (newCard && !art.isDefault()) {
            cardArtRepository.findByCardIdAndSource(cardId, ArtSource.DEFAULT)
                .filter(d -> !playerArtRepository.existsByUserIdAndArtId(userId, d.getId()))
                .ifPresent(d -> saveOwnedArt(userId, d));
        }
        return result(pc, true, newCard, 0);
    }

    private GrantResult result(PlayerCard pc, boolean newArt, boolean newCard, int fragmentsGained) {
        return new GrantResult(newArt, newCard, fragmentsGained,
            pc.getFragments(), pc.getLevel(), GameRules.nextUpgradeCost(pc.getLevel()));
    }

    private PlayerCard createPlayerCard(String userId, CardArt art) {
        PlayerCard pc = new PlayerCard();
        pc.setUserId(userId);
        pc.setCard(art.getCard());
        pc.setEquippedArt(art);
        return playerCardRepository.save(pc);
    }

    private void saveOwnedArt(String userId, CardArt art) {
        PlayerArt pa = new PlayerArt();
        pa.setUserId(userId);
        pa.setArt(art);
        playerArtRepository.save(pa);
    }

    private PlayerCard getPlayerCard(String userId, String cardPublicId) {
        return playerCardRepository.findByUserIdAndCardPublicId(userId, cardPublicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not owned"));
    }
}
