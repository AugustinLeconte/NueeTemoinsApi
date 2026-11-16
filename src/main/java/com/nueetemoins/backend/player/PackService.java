package com.nueetemoins.backend.player;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nueetemoins.backend.card.ArtSource;
import com.nueetemoins.backend.card.Card;
import com.nueetemoins.backend.card.CardArt;
import com.nueetemoins.backend.card.CardArtRepository;
import com.nueetemoins.backend.card.CardArtService;
import com.nueetemoins.backend.card.CardRarity;
import com.nueetemoins.backend.player.dto.DrawnItem;
import com.nueetemoins.backend.player.dto.PackResult;

import lombok.RequiredArgsConstructor;

/**
 * Chaque emplacement du pack est soit des pièces, soit une illustration.
 * Illustration : tirage en 2 étapes (1) un saint, boosté le jour de sa fête ; (2) une illustration selon la rareté.
 * Garanties : un pack contient toujours au moins une carte, et le slot "pity" est toujours une carte.
 */
@Service
@RequiredArgsConstructor
public class PackService {

    private static final Set<ArtSource> POOL_SOURCES = Set.of(ArtSource.DEFAULT, ArtSource.PACK);
    private static final String DEFAULT_COUNTRY = "FR";

    private final CardArtRepository artRepository;
    private final PlayerCollectionService collection;
    private final CardArtService cardArtService;

    private final Random random = new SecureRandom();

    /** Pack quotidien gratuit (1 par jour, fuseau Europe/Paris). */
    @Transactional
    public PackResult openDaily(String userId, String lang, String country) {
        PlayerWallet wallet = collection.lockWallet(userId);
        LocalDate today = LocalDate.now(PlayerCollectionService.ZONE);
        if (today.equals(wallet.getLastDailyPack())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Daily pack already opened");
        }
        wallet.setLastDailyPack(today);
        return open(userId, wallet, PackType.STANDARD, lang, country);
    }

    /** Point d'entrée réutilisable (pack acheté, offert par un événement...). */
    @Transactional
    public PackResult open(String userId, PlayerWallet wallet, PackType type, String lang, String country) {
        List<CardArt> pool = artRepository.findPackPool(POOL_SOURCES);
        if (pool.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No art available");
        }
        Map<UUID, List<CardArt>> byCard = pool.stream()
            .collect(Collectors.groupingBy(a -> a.getCard().getId()));

        boolean pityActive = wallet.getPacksSinceRare() >= GameRules.PITY_THRESHOLD;
        boolean gotRare = false;
        int cardsDrawn = 0;
        int newCards = 0;
        int newArts = 0;
        int levelBefore = GameRules.levelForXp(wallet.getXp());
        int coinsTotal = 0;
        List<DrawnItem> draws = new ArrayList<>();

        for (int i = 0; i < type.size; i++) {
            boolean last = i == type.size - 1;
            boolean pitySlot = pityActive && !gotRare && last;
            boolean mustBeCard = pitySlot || (last && cardsDrawn == 0);

            // --- emplacement "pièces" ---
            if (!mustBeCard && random.nextInt(100) < type.coinChancePercent) {
                int amount = type.coinMin + random.nextInt(type.coinMax - type.coinMin + 1);
                wallet.setCoins(wallet.getCoins() + amount);
                coinsTotal += amount;
                draws.add(DrawnItem.ofCoins(amount));
                continue;
            }

            // --- emplacement "carte" ---
            CardRarity min = pitySlot ? GameRules.PITY_MIN : CardRarity.COMMON;
            CardRarity target = rollRarity(type, min);
            CardArt art = pickArt(byCard, target, min, country);
            if (art.getRarity().ordinal() >= GameRules.PITY_MIN.ordinal()) {
                gotRare = true;
            }
            GrantResult g = collection.grantArt(userId, art);
            cardsDrawn++;
            if (g.newCard()) newCards++; else if (g.newArt()) newArts++;
            draws.add(DrawnItem.ofArt(cardArtService.toResponse(art, lang),
                g.newArt(), g.newCard(), g.fragmentsGained(),
                g.cardFragments(), g.cardLevel(), g.nextUpgradeCost()));
        }

        wallet.setPacksSinceRare(gotRare ? 0 : wallet.getPacksSinceRare() + 1);
        int xpGained = GameRules.XP_PACK_OPENED
            + newCards * GameRules.XP_NEW_CARD + newArts * GameRules.XP_NEW_ART;
        collection.addXp(wallet, xpGained);
        return new PackResult(type.name(), draws, coinsTotal, wallet.getCoins(), wallet.getPacksSinceRare(),
            xpGained, levelBefore, collection.progressOf(wallet));
    }

    // ---------- Algorithme ----------

    private CardRarity rollRarity(PackType type, CardRarity min) {
        List<CardRarity> candidates = new ArrayList<>();
        for (CardRarity r : CardRarity.values()) {
            if (r.ordinal() >= min.ordinal()) candidates.add(r);
        }
        return weighted(candidates, type::weightOf);
    }

    private CardArt pickArt(Map<UUID, List<CardArt>> byCard, CardRarity target, CardRarity min, String country) {
        List<List<CardArt>> saints = byCard.values().stream()
            .filter(arts -> arts.stream().anyMatch(a -> a.getRarity().ordinal() >= min.ordinal()))
            .toList();
        List<CardArt> arts = weighted(saints, l -> saintWeight(l.get(0).getCard(), country));

        for (CardRarity r : searchOrder(target, min)) {
            List<CardArt> sameRarity = arts.stream().filter(a -> a.getRarity() == r).toList();
            if (!sameRarity.isEmpty()) {
                return weighted(sameRarity, CardArt::getDropWeight);
            }
        }
        throw new IllegalStateException("No art found for target " + target);
    }

    private List<CardRarity> searchOrder(CardRarity target, CardRarity min) {
        List<CardRarity> order = new ArrayList<>();
        for (int i = target.ordinal(); i >= min.ordinal(); i--) order.add(CardRarity.values()[i]);
        for (int i = target.ordinal() + 1; i < CardRarity.values().length; i++) order.add(CardRarity.values()[i]);
        return order;
    }

    private int saintWeight(Card card, String country) {
        MonthDay today = MonthDay.now(PlayerCollectionService.ZONE);
        boolean feast = card.findFeastDate(country)
            .or(() -> card.findFeastDate(DEFAULT_COUNTRY))
            .filter(f -> f.getMonth() == today.getMonthValue() && f.getDay() == today.getDayOfMonth())
            .isPresent();
        return feast ? GameRules.FEAST_BOOST : 1;
    }

    private <T> T weighted(List<T> items, ToIntFunction<T> weight) {
        int sum = items.stream().mapToInt(weight).sum();
        if (sum <= 0) return items.get(random.nextInt(items.size()));
        int roll = random.nextInt(sum);
        for (T item : items) {
            roll -= weight.applyAsInt(item);
            if (roll < 0) return item;
        }
        return items.get(items.size() - 1);
    }
}
