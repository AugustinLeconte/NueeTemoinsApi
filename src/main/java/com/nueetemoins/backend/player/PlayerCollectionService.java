package com.nueetemoins.backend.player;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
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

    // ---------- Wallet ----------

    /** Récupère (ou crée) le wallet avec un verrou en écriture. */
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
        return new WalletResponse(w.getFragments(), w.getPacksSinceRare(), available);
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
            cardArtService.toResponse(pc.getEquippedArt(), lang),
            owned.getOrDefault(pc.getCard().getId(), List.of()).stream()
                .map(a -> cardArtService.toResponse(a, lang)).toList(),
            pc.getLevel() < GameRules.MAX_LEVEL ? GameRules.upgradeCost(pc.getLevel()) : null
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

    @Transactional
    public UpgradeResponse upgrade(String userId, String cardPublicId) {
        PlayerWallet wallet = lockWallet(userId);
        PlayerCard pc = getPlayerCard(userId, cardPublicId);
        if (pc.getLevel() >= GameRules.MAX_LEVEL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Max level reached");
        }
        int cost = GameRules.upgradeCost(pc.getLevel());
        if (wallet.getFragments() < cost) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough fragments");
        }
        wallet.setFragments(wallet.getFragments() - cost);
        pc.setLevel(pc.getLevel() + 1);
        return new UpgradeResponse(pc.getLevel(), wallet.getFragments());
    }

    // ---------- Boutique ----------

    @Transactional(readOnly = true)
    public List<ShopItemResponse> shop(String userId, String lang) {
        Set<UUID> owned = playerArtRepository.findOwnedArtIds(userId);
        return cardArtRepository.findBySourceWithCard(ArtSource.SHOP).stream()
            .filter(a -> a.getFragmentPrice() != null)
            .map(a -> new ShopItemResponse(
                cardArtService.toResponse(a, lang), a.getFragmentPrice(), owned.contains(a.getId())))
            .toList();
    }

    @Transactional
    public WalletResponse buy(String userId, String artPublicId) {
        CardArt art = cardArtRepository.findByPublicId(artPublicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Art not found"));
        if (art.getSource() != ArtSource.SHOP || !art.isObtainable() || art.getFragmentPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Art is not for sale");
        }
        PlayerWallet wallet = lockWallet(userId);
        if (playerArtRepository.existsByUserIdAndArtId(userId, art.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Art already owned");
        }
        if (wallet.getFragments() < art.getFragmentPrice()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough fragments");
        }
        wallet.setFragments(wallet.getFragments() - art.getFragmentPrice());
        grantArt(userId, wallet, art);
        return toResponse(wallet);
    }

    // ---------- Attribution ----------

    /**
     * Donne une illustration au joueur.
     * - déjà possédée : conversion en fragments (sur le wallet fourni)
     * - nouveau saint : création du PlayerCard + illustration par défaut offerte
     */
    @Transactional
    public GrantResult grantArt(String userId, PlayerWallet wallet, CardArt art) {
        if (playerArtRepository.existsByUserIdAndArtId(userId, art.getId())) {
            int fragments = GameRules.duplicateFragments(art.getRarity());
            wallet.setFragments(wallet.getFragments() + fragments);
            return new GrantResult(false, false, fragments);
        }
        saveOwnedArt(userId, art);

        UUID cardId = art.getCard().getId();
        boolean newCard = playerCardRepository.findByUserIdAndCardId(userId, cardId).isEmpty();
        if (newCard) {
            if (!art.isDefault()) {
                cardArtRepository.findByCardIdAndSource(cardId, ArtSource.DEFAULT)
                    .filter(d -> !playerArtRepository.existsByUserIdAndArtId(userId, d.getId()))
                    .ifPresent(d -> saveOwnedArt(userId, d));
            }
            PlayerCard pc = new PlayerCard();
            pc.setUserId(userId);
            pc.setCard(art.getCard());
            pc.setEquippedArt(art);
            playerCardRepository.save(pc);
        }
        return new GrantResult(true, newCard, 0);
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
