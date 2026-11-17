package com.nueetemoins.backend.minigame.wordle;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nueetemoins.backend.minigame.MiniGameResult;
import com.nueetemoins.backend.minigame.MiniGameResultRepository;
import com.nueetemoins.backend.minigame.MiniGameType;
import com.nueetemoins.backend.minigame.wordle.dto.WordleDto.GameState;
import com.nueetemoins.backend.minigame.wordle.dto.WordleDto.GuessRow;
import com.nueetemoins.backend.minigame.wordle.dto.WordleDto.LeaderboardEntry;
import com.nueetemoins.backend.minigame.wordle.dto.WordleDto.LetterState;
import com.nueetemoins.backend.player.GameRules;
import com.nueetemoins.backend.player.PlayerProgressService;
import com.nueetemoins.backend.profile.ProfileService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WordleService {

    public static final int MAX_ATTEMPTS = 6;
    private static final int LEADERBOARD_SIZE = 50;

    private final WordleGameRepository games;
    private final MiniGameResultRepository results;
    private final WordBank bank;
    private final PlayerProgressService progressService;
    private final ProfileService profiles;

    private static LocalDate today() {
        return LocalDate.now(GameRules.GAME_ZONE);
    }

    @Transactional(readOnly = true)
    public GameState today(String userId, String lang) {
        return games.findByUserIdAndGameDate(userId, today())
            .map(this::toState)
            .orElseGet(() -> new GameState(bank.resolveLang(lang), bank.lengthForDay(today()),
                MAX_ATTEMPTS, WordleGame.Status.IN_PROGRESS, List.of(), null, 0));
    }

    @Transactional
    public GameState guess(String userId, String lang, String rawGuess) {
        LocalDate day = today();
        WordleGame game = games.findByUserIdAndGameDate(userId, day).orElseGet(() -> {
            WordleGame g = new WordleGame();
            g.setUserId(userId);
            g.setGameDate(day);
            g.setLang(bank.resolveLang(lang));
            g.setWordLength(bank.lengthForDay(day));
            return games.save(g);
        });

        if (game.getStatus() != WordleGame.Status.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ALREADY_FINISHED");
        }
        String word = WordBank.normalize(rawGuess);
        if (word.length() != game.getWordLength()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "WRONG_LENGTH");
        }
        if (!bank.isValid(game.getLang(), game.getWordLength(), word)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "UNKNOWN_WORD");
        }

        game.getGuesses().add(word);
        String solution = bank.dailyWord(game.getLang(), game.getWordLength(), day);
        boolean won = word.equals(solution);
        int attempts = game.getGuesses().size();

        if (won || attempts >= MAX_ATTEMPTS) {
            int xp = GameRules.wordleXp(won, attempts);
            game.setStatus(won ? WordleGame.Status.WON : WordleGame.Status.LOST);
            game.setXpAwarded(xp);
            game.setFinishedAt(Instant.now());

            MiniGameResult r = new MiniGameResult();
            r.setUserId(userId);
            r.setGame(MiniGameType.WORDLE);
            r.setPlayedOn(day);
            r.setWon(won);
            r.setPerfect(won && attempts == 1);
            r.setXp(xp);
            results.save(r); // doit être enregistré AVANT awardXp (qui vérifie les succès)
            games.save(game);
            progressService.awardXp(userId, xp);
        } else {
            games.save(game);
        }
        return toState(game);
    }

    /** Classement du jour (même langue), réservé à ceux qui ont terminé (anti-spoiler). */
    @Transactional(readOnly = true)
    public List<LeaderboardEntry> leaderboard(String userId) {
        LocalDate day = today();
        WordleGame mine = games.findByUserIdAndGameDate(userId, day)
            .filter(g -> g.getFinishedAt() != null)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "FINISH_FIRST"));

        List<WordleGame> top = games.findLeaderboard(day, mine.getLang(), PageRequest.of(0, LEADERBOARD_SIZE));
        Map<String, String> pseudos = profiles.pseudosOf(top.stream().map(WordleGame::getUserId).toList());
        return IntStream.range(0, top.size()).mapToObj(i -> {
            WordleGame g = top.get(i);
            return new LeaderboardEntry(i + 1,
                pseudos.getOrDefault(g.getUserId(), "?"),
                g.getStatus() == WordleGame.Status.WON, g.getGuesses().size(), g.getXpAwarded());
        }).toList();
    }

    private GameState toState(WordleGame g) {
        String solution = bank.dailyWord(g.getLang(), g.getWordLength(), g.getGameDate());
        boolean finished = g.getStatus() != WordleGame.Status.IN_PROGRESS;
        List<GuessRow> rows = g.getGuesses().stream()
            .map(w -> new GuessRow(w, evaluate(w, solution))).toList();
        return new GameState(g.getLang(), g.getWordLength(), MAX_ATTEMPTS, g.getStatus(),
            rows, finished ? solution : null, g.getXpAwarded());
    }

    /** Algorithme Wordle standard (gère correctement les lettres en double). */
    static List<LetterState> evaluate(String guess, String solution) {
        int n = guess.length();
        LetterState[] res = new LetterState[n];
        Arrays.fill(res, LetterState.ABSENT);
        int[] remaining = new int[26];
        for (int i = 0; i < n; i++) {
            if (guess.charAt(i) == solution.charAt(i)) res[i] = LetterState.CORRECT;
            else remaining[solution.charAt(i) - 'A']++;
        }
        for (int i = 0; i < n; i++) {
            if (res[i] == LetterState.CORRECT) continue;
            int c = guess.charAt(i) - 'A';
            if (remaining[c] > 0) { res[i] = LetterState.PRESENT; remaining[c]--; }
        }
        return new ArrayList<>(Arrays.asList(res));
    }
}