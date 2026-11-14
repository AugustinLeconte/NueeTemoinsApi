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
import com.nueetemoins.backend.player.dto.DrawnArt;
import com.nueetemoins.backend.player.dto.PackResult;

import lombok.RequiredArgsConstructor;

/**
 * Tirage en 2 étapes : 1) un saint (poids 1, boosté le jour de sa fête)
 *                      2) une illustration de ce saint selon la rareté tirée.
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
        int totalFragments = 0;
        List<DrawnArt> draws = new ArrayList<>();

        for (int i = 0; i < type.size; i++) {
            CardRarity min = CardRarity.COMMON;
            if (pityActive && !gotRare && i == type.size - 1) {
                min = GameRules.PITY_MIN; // dernier slot garanti
            }
            CardRarity target = rollRarity(type, min);
            CardArt art = pickArt(byCard, target, min, country);
            if (art.getRarity().ordinal() >= GameRules.PITY_MIN.ordinal()) {
                gotRare = true;
            }
            GrantResult g = collection.grantArt(userId, wallet, art);
            totalFragments += g.fragmentsGained();
            draws.add(new DrawnArt(cardArtService.toResponse(art, lang),
                g.newArt(), g.newCard(), g.fragmentsGained()));
        }

        wallet.setPacksSinceRare(gotRare ? 0 : wallet.getPacksSinceRare() + 1);
        return new PackResult(draws, totalFragments, wallet.getFragments(), wallet.getPacksSinceRare());
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
        // 1) le saint : uniquement ceux ayant au moins une illustration >= min
        List<List<CardArt>> saints = byCard.values().stream()
            .filter(arts -> arts.stream().anyMatch(a -> a.getRarity().ordinal() >= min.ordinal()))
            .toList();
        List<CardArt> arts = weighted(saints, l -> saintWeight(l.get(0).getCard(), country));

        // 2) l'illustration : rareté cible, sinon la plus proche en descendant jusqu'à min, puis en montant
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
