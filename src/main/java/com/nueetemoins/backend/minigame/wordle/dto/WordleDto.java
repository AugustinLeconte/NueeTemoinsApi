package com.nueetemoins.backend.minigame.wordle.dto;

import java.util.List;

import com.nueetemoins.backend.minigame.wordle.WordleGame;

import jakarta.validation.constraints.NotBlank;

public final class WordleDto {
    private WordleDto() {}

    public enum LetterState { CORRECT, PRESENT, ABSENT }

    public record GuessRow(String word, List<LetterState> states) {}

    /** `solution` n'est renseignée qu'une fois la partie terminée. */
    public record GameState(String lang, int wordLength, int maxAttempts,
                            WordleGame.Status status, List<GuessRow> guesses,
                            String solution, int xpAwarded, long durationMs) {}

    public record GuessRequest(@NotBlank String guess) {}

    public record LeaderboardEntry(int rank, String player, boolean won, int attempts, int xp, long durationMs) {}
}