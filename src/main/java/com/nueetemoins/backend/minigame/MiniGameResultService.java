package com.nueetemoins.backend.minigame;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nueetemoins.backend.player.GameRules;
import com.nueetemoins.backend.player.PlayerProgressService;
import com.nueetemoins.backend.profile.ProfileService;

import lombok.RequiredArgsConstructor;

/** Point d'entrée commun : enregistre un résultat + crédite l'XP (et déclenche les succès) + classement. */
@Service
@RequiredArgsConstructor
public class MiniGameResultService {

    private static final int LEADERBOARD_SIZE = 50;

    private final MiniGameResultRepository repo;
    private final PlayerProgressService progressService;
    private final ProfileService profiles;

    public record LeaderboardRow(int rank, String player, boolean won, int attempts, int xp, long durationMs) {}

    /** durationMs / attempts peuvent être null (ex. Wordle). */
    @Transactional
    public void record(String userId, MiniGameType game, LocalDate day, boolean won, boolean perfect,
                       int xp, Long durationMs, Integer attempts) {
        MiniGameResult r = new MiniGameResult();
        r.setUserId(userId);
        r.setGame(game);
        r.setPlayedOn(day);
        r.setWon(won);
        r.setPerfect(perfect);
        r.setXp(xp);
        r.setDurationMs(durationMs);
        r.setAttempts(attempts);
        repo.save(r); // doit être enregistré AVANT awardXp (qui vérifie les succès)
        progressService.awardXp(userId, xp);
    }

    /** Classement du jour, réservé à ceux qui ont terminé ce jeu (anti-spoiler, comme le Wordle). */
    @Transactional(readOnly = true)
    public List<LeaderboardRow> leaderboard(String userId, MiniGameType game) {
        LocalDate day = LocalDate.now(GameRules.GAME_ZONE);
        if (!repo.existsByUserIdAndGameAndPlayedOn(userId, game, day)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FINISH_FIRST");
        }
        List<MiniGameResult> rows = repo.findByGameAndPlayedOnOrderByXpDescDurationMsAscCreatedAtAsc(
            game, day, PageRequest.of(0, LEADERBOARD_SIZE));
        Map<String, String> pseudos = profiles.pseudosOf(rows.stream().map(MiniGameResult::getUserId).toList());
        return IntStream.range(0, rows.size()).mapToObj(i -> {
            MiniGameResult r = rows.get(i);
            return new LeaderboardRow(i + 1, pseudos.getOrDefault(r.getUserId(), "?"), r.isWon(),
                r.getAttempts() == null ? 0 : r.getAttempts(), r.getXp(),
                r.getDurationMs() == null ? 0 : r.getDurationMs());
        }).toList();
    }
}