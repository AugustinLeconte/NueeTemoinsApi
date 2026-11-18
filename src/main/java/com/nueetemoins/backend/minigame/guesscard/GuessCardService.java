package com.nueetemoins.backend.minigame.guesscard;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nueetemoins.backend.card.Card;
import com.nueetemoins.backend.card.CardRepository;
import com.nueetemoins.backend.minigame.DailyChallengeService;
import com.nueetemoins.backend.minigame.MiniGameResultService;
import com.nueetemoins.backend.minigame.MiniGameType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GuessCardService {

    static final int LEVELS = DailyChallengeService.GUESS_ROUNDS;
    static final int MAX_GUESSES = 3;
    static final int XP_PER_LEVEL = 15;
    static final int PENALTY = 5;
    private static final double[] ZOOM = {4.0, 2.8, 1.9};
    private static final int[] ROTATIONS = {90, 180, 270};
    private static final long MAX_GAP_MS = 60_000;

    private final GuessCardGameRepository games;
    private final DailyChallengeService challenges;
    private final CardRepository cardRepository;
    private final MiniGameResultService results;

    public record CardName(String id, String name) {}

    public record RoundView(int level, String status, boolean grayscale, int rotation, double zoom,
                            double originX, double originY, int wrongCount, String imageUrl,
                            List<CardName> wrong, CardName answer, int xp) {}

    public record StateView(String status, int maxGuesses, int currentLevel, List<RoundView> rounds,
                            int xpAwarded, long durationMs) {}

    @Transactional
    public StateView today(String userId, String lang) {
        return view(loadOrCreate(userId), lang);
    }

    @Transactional
    public StateView guess(String userId, String cardId, String lang) {
        GuessCardGame g = loadOrCreate(userId);
        int level = currentLevel(g);
        if (level == 0) throw err(HttpStatus.CONFLICT, "ALREADY_FINISHED");
        if (!cardRepository.existsByPublicId(cardId)) throw err(HttpStatus.UNPROCESSABLE_ENTITY, "UNKNOWN_CARD");
        if (g.getAttempts().stream().anyMatch(a -> a.getLevel() == level && a.getCardPublicId().equals(cardId))) {
            throw err(HttpStatus.UNPROCESSABLE_ENTITY, "ALREADY_GUESSED");
        }

        tick(g);
        String target = cardIds(g).get(level - 1);
        g.getAttempts().add(new GuessCardGame.Attempt(level, cardId, target.equals(cardId)));
        if (currentLevel(g) == 0) finish(g);
        return view(g, lang);
    }

    /** Liste d'auto-complétion : TOUTES les cartes (sinon la liste trahirait la réponse). */
    @Transactional(readOnly = true)
    public List<CardName> names(String lang) {
        return cardRepository.findAll().stream()
            .map(c -> new CardName(c.getPublicId(), DailyChallengeService.name(c, lang)))
            .sorted(Comparator.comparing(CardName::name, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    // ---- logique de jeu ----
    private static int wrong(GuessCardGame g, int level) {
        return (int) g.getAttempts().stream().filter(a -> a.getLevel() == level && !a.isCorrect()).count();
    }
    private static boolean solved(GuessCardGame g, int level) {
        return g.getAttempts().stream().anyMatch(a -> a.getLevel() == level && a.isCorrect());
    }
    private static boolean done(GuessCardGame g, int level) {
        return solved(g, level) || wrong(g, level) >= MAX_GUESSES;
    }
    private static int roundXp(GuessCardGame g, int level) {
        return solved(g, level) ? XP_PER_LEVEL - PENALTY * wrong(g, level) : 0;
    }
    private static int currentLevel(GuessCardGame g) {
        for (int l = 1; l <= LEVELS; l++) if (!done(g, l)) return l;
        return 0;
    }

    private void finish(GuessCardGame g) {
        int xp = 0, solvedCount = 0, wrongTotal = 0;
        for (int l = 1; l <= LEVELS; l++) {
            xp += roundXp(g, l);
            if (solved(g, l)) solvedCount++;
            wrongTotal += wrong(g, l);
        }
        g.setFinishedAt(Instant.now());
        results.record(g.getUserId(), MiniGameType.GUESS_CARD, g.getPlayedOn(),
            solvedCount >= 2,                         // "gagné" = au moins 2 cartes sur 3
            solvedCount == LEVELS && wrongTotal == 0, // "parfait" = 3/3 sans aucune erreur
            xp, g.getActiveMs(), g.getAttempts().size());
    }

    private void tick(GuessCardGame g) {
        Instant now = Instant.now();
        long gap = Duration.between(g.getLastActionAt(), now).toMillis();
        g.setActiveMs(g.getActiveMs() + Math.max(0, Math.min(gap, MAX_GAP_MS)));
        g.setLastActionAt(now);
    }

    private GuessCardGame loadOrCreate(String userId) {
        LocalDate day = DailyChallengeService.today();
        return games.findByUserIdAndPlayedOn(userId, day).orElseGet(() -> {
            GuessCardGame g = new GuessCardGame();
            g.setUserId(userId);
            g.setPlayedOn(day);
            return games.save(g);
        });
    }

    private List<String> cardIds(GuessCardGame g) {
        return challenges.getOrCreate(MiniGameType.GUESS_CARD, g.getPlayedOn()).getCardPublicIds();
    }

    private StateView view(GuessCardGame g, String lang) {
        List<String> ids = cardIds(g);
        int current = currentLevel(g);
        List<RoundView> rounds = new ArrayList<>();
        int totalXp = 0;

        for (int l = 1; l <= LEVELS; l++) {
            boolean locked = current != 0 && l > current;
            boolean isDone = done(g, l);
            int wrong = wrong(g, l);
            totalXp += roundXp(g, l);

            String status = locked ? "LOCKED" : !isDone ? "IN_PROGRESS" : solved(g, l) ? "WON" : "LOST";
            if (locked) {
                rounds.add(new RoundView(l, status, false, 0, ZOOM[0], 50, 50, 0, null, List.of(), null, 0));
                continue;
            }
            // Effets déterministes par jour+niveau (identiques pour tous les joueurs)
            Random r = new Random(g.getPlayedOn().toEpochDay() * 7 + l);
            double ox = 25 + r.nextInt(51), oy = 25 + r.nextInt(51);
            int rot = l >= 3 ? ROTATIONS[r.nextInt(ROTATIONS.length)] : 0;
            boolean gray = l >= 2;

            Card target = card(ids.get(l - 1));
            final int level = l;
            List<CardName> wrongNames = g.getAttempts().stream()
                .filter(a -> a.getLevel() == level && !a.isCorrect())
                .map(a -> toName(card(a.getCardPublicId()), lang)).toList();

            if (isDone) { // révélation : carte entière, à l'endroit, en couleur
                rounds.add(new RoundView(l, status, false, 0, 1.0, 50, 50, wrong,
                    DailyChallengeService.image(target), wrongNames, toName(target, lang), roundXp(g, l)));
            } else {
                rounds.add(new RoundView(l, status, gray, rot, ZOOM[wrong], ox, oy, wrong,
                    DailyChallengeService.image(target), wrongNames, null, 0));
            }
        }
        return new StateView(current == 0 ? "FINISHED" : "IN_PROGRESS", MAX_GUESSES, current, rounds,
            totalXp, g.getActiveMs());
    }

    private Card card(String publicId) {
        return cardRepository.findByPublicId(publicId)
            .orElseThrow(() -> err(HttpStatus.INTERNAL_SERVER_ERROR, "CARD_MISSING"));
    }
    private static CardName toName(Card c, String lang) {
        return new CardName(c.getPublicId(), DailyChallengeService.name(c, lang));
    }
    private static ResponseStatusException err(HttpStatus s, String code) {
        return new ResponseStatusException(s, code);
    }
}