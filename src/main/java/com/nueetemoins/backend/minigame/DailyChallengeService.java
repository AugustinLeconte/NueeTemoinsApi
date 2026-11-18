package com.nueetemoins.backend.minigame;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nueetemoins.backend.card.Card;
import com.nueetemoins.backend.card.CardArt;
import com.nueetemoins.backend.card.CardRepository;
import com.nueetemoins.backend.card.CardTranslation;
import com.nueetemoins.backend.player.GameRules;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DailyChallengeService {

    public static final int GUESS_ROUNDS = 3;
    public static final int MEMORY_PAIRS = 8; // plateau 4x4

    private final DailyChallengeRepository repo;
    private final CardRepository cardRepository;

    public static LocalDate today() { return LocalDate.now(GameRules.GAME_ZONE); }

    /**
     * NB : si deux premières requêtes arrivent exactement en même temps, la contrainte unique
     * (game, day) en fait échouer une. Rare ; en cas de souci, ajouter un retry sur la 1re requête du jour.
     */
    @Transactional
    public DailyChallenge getOrCreate(MiniGameType game, LocalDate day) {
        return repo.findByGameAndDay(game, day).orElseGet(() -> repo.save(build(game, day)));
    }

    private DailyChallenge build(MiniGameType game, LocalDate day) {
        // Graine fixe par jour+jeu : tirage reproductible
        Random rnd = new Random(day.toEpochDay() * 31 + game.name().hashCode());
        // Seules les cartes avec une illustration par défaut sont jouables, triées pour un tirage stable
        List<Card> eligible = cardRepository.findAll().stream()
            .filter(c -> c.findDefaultArt().isPresent())
            .sorted(Comparator.comparing(Card::getPublicId))
            .toList();

        List<String> ids = switch (game) {
            case GUESS_CARD -> pickGuess(eligible, rnd);
            case MEMORY -> pickMemory(eligible, rnd);
            default -> throw new IllegalArgumentException("No daily challenge for " + game);
        };
        DailyChallenge c = new DailyChallenge();
        c.setGame(game);
        c.setDay(day);
        c.setCardPublicIds(ids);
        return c;
    }

    private List<String> pickGuess(List<Card> eligible, Random rnd) {
        if (eligible.size() < GUESS_ROUNDS) throw notEnoughCards();
        List<Card> pool = new ArrayList<>(eligible);
        Collections.shuffle(pool, rnd);
        return pool.stream().limit(GUESS_ROUNDS).map(Card::getPublicId).toList();
    }

    /** Priorité à une même famille ; complète avec d'autres cartes s'il n'y en a pas assez. */
    private List<String> pickMemory(List<Card> eligible, Random rnd) {
        Map<String, List<Card>> byFamily = new TreeMap<>();
        for (Card c : eligible) {
            c.getFamilies().forEach(f ->
                byFamily.computeIfAbsent(f.getPublicId(), k -> new ArrayList<>()).add(c));
        }
        List<List<Card>> full = byFamily.values().stream().filter(l -> l.size() >= MEMORY_PAIRS).toList();
        List<Card> family = !full.isEmpty()
            ? full.get(rnd.nextInt(full.size()))
            : byFamily.values().stream().max(Comparator.comparingInt(List::size)).orElse(List.of());

        List<Card> shuffled = new ArrayList<>(family);
        Collections.shuffle(shuffled, rnd);

        List<String> picked = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Card c : shuffled) {
            if (picked.size() == MEMORY_PAIRS) break;
            if (seen.add(c.getPublicId())) picked.add(c.getPublicId());
        }
        if (picked.size() < MEMORY_PAIRS) {
            List<Card> rest = new ArrayList<>(eligible);
            Collections.shuffle(rest, rnd);
            for (Card c : rest) {
                if (picked.size() == MEMORY_PAIRS) break;
                if (seen.add(c.getPublicId())) picked.add(c.getPublicId());
            }
        }
        if (picked.size() < MEMORY_PAIRS) throw notEnoughCards();

        List<String> board = new ArrayList<>();
        for (String id : picked) { board.add(id); board.add(id); }
        Collections.shuffle(board, rnd);
        return board;
    }

    private static ResponseStatusException notEnoughCards() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "NOT_ENOUGH_CARDS");
    }

    // ---- helpers partagés par les deux jeux ----

    /** "fr-FR" / "FR" / null -> "fr". */
    private static String normLang(String lang) {
        return (lang == null || lang.length() < 2) ? "fr" : lang.substring(0, 2).toLowerCase(Locale.ROOT);
    }

    public static String name(Card c, String lang) {
        return c.findTranslation(normLang(lang))
            .or(() -> c.findTranslation("fr"))
            .map(CardTranslation::getTitle)
            .orElse(c.getPublicId());
    }

    public static String image(Card c) {
        return c.findDefaultArt().map(CardArt::getImageUrl).orElse(null);
    }
}