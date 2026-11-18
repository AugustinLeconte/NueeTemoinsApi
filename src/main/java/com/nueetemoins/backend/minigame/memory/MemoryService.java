package com.nueetemoins.backend.minigame.memory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

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

/**
 * Le plateau n'est JAMAIS envoyé en entier : le serveur ne révèle une carte que quand on la retourne,
 * donc impossible de tricher en regardant la réponse réseau.
 */
@Service
@RequiredArgsConstructor
public class MemoryService {

    private static final int MAX_XP = 30;
    private static final int MIN_XP = 10;
    private static final int XP_LOST_PER_EXTRA_MOVE = 2;
    private static final long MAX_GAP_MS = 30_000;

    private final MemoryGameRepository games;
    private final DailyChallengeService challenges;
    private final CardRepository cardRepository;
    private final MiniGameResultService results;

    public record Tile(int index, String cardId, String imageUrl, String name) {}

    /** status : IN_PROGRESS | FINISHED. matched = uniquement les paires trouvées. */
    public record MemoryState(int size, String status, int moves, List<Tile> matched, Tile pending,
                              int xpAwarded, long durationMs) {}

    /** match : null = 1re carte, true/false = résultat de la paire. firstIndex : 1re carte de la paire. */
    public record FlipResult(Tile revealed, Integer firstIndex, Boolean match, MemoryState state) {}

    @Transactional
    public MemoryState today(String userId, String lang) {
        MemoryGame g = loadOrCreate(userId);
        return view(g, board(g), lang);
    }

    @Transactional
    public FlipResult flip(String userId, int index, String lang) {
        MemoryGame g = loadOrCreate(userId);
        List<String> board = board(g);
        if (g.isFinished()) throw err(HttpStatus.CONFLICT, "ALREADY_FINISHED");
        if (index < 0 || index >= board.size() || g.getMatched().contains(index)
            || Objects.equals(g.getPendingIndex(), index)) {
            throw err(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_TILE");
        }
        tick(g);
        Tile revealed = tile(index, board.get(index), lang);

        if (g.getPendingIndex() == null) { // 1re carte de la paire
            g.setPendingIndex(index);
            return new FlipResult(revealed, null, null, view(g, board, lang));
        }
        int first = g.getPendingIndex();   // 2e carte : on résout la paire
        g.setPendingIndex(null);
        g.setMoves(g.getMoves() + 1);
        boolean match = board.get(first).equals(board.get(index));
        if (match) {
            g.getMatched().add(first);
            g.getMatched().add(index);
            if (g.getMatched().size() == board.size()) finish(g, board.size() / 2);
        }
        return new FlipResult(revealed, first, match, view(g, board, lang));
    }

    private void finish(MemoryGame g, int pairs) {
        int xp = Math.max(MIN_XP, MAX_XP - XP_LOST_PER_EXTRA_MOVE * (g.getMoves() - pairs));
        g.setXpAwarded(xp);
        g.setFinishedAt(Instant.now());
        results.record(g.getUserId(), MiniGameType.MEMORY, g.getPlayedOn(),
            true, g.getMoves() == pairs, xp, g.getActiveMs(), g.getMoves());
    }

    private void tick(MemoryGame g) {
        Instant now = Instant.now();
        long gap = Duration.between(g.getLastActionAt(), now).toMillis();
        g.setActiveMs(g.getActiveMs() + Math.max(0, Math.min(gap, MAX_GAP_MS)));
        g.setLastActionAt(now);
    }

    private MemoryGame loadOrCreate(String userId) {
        LocalDate day = DailyChallengeService.today();
        return games.findByUserIdAndPlayedOn(userId, day).orElseGet(() -> {
            MemoryGame g = new MemoryGame();
            g.setUserId(userId);
            g.setPlayedOn(day);
            return games.save(g);
        });
    }

    private List<String> board(MemoryGame g) {
        return challenges.getOrCreate(MiniGameType.MEMORY, g.getPlayedOn()).getCardPublicIds();
    }

    private MemoryState view(MemoryGame g, List<String> board, String lang) {
        List<Tile> matched = g.getMatched().stream().sorted()
            .map(i -> tile(i, board.get(i), lang)).toList();
        Tile pending = g.getPendingIndex() == null ? null
            : tile(g.getPendingIndex(), board.get(g.getPendingIndex()), lang);
        return new MemoryState(board.size(), g.isFinished() ? "FINISHED" : "IN_PROGRESS", g.getMoves(),
            matched, pending, g.getXpAwarded(), g.getActiveMs());
    }

    private Tile tile(int index, String cardId, String lang) {
        Card c = cardRepository.findByPublicId(cardId)
            .orElseThrow(() -> err(HttpStatus.INTERNAL_SERVER_ERROR, "CARD_MISSING"));
        return new Tile(index, cardId, DailyChallengeService.image(c), DailyChallengeService.name(c, lang));
    }

    private static ResponseStatusException err(HttpStatus s, String code) {
        return new ResponseStatusException(s, code);
    }
}