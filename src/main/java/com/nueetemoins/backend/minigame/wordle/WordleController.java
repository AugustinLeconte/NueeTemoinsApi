package com.nueetemoins.backend.minigame.wordle;

import java.util.List;
import java.util.Locale;

import org.springframework.web.bind.annotation.*;

import com.nueetemoins.backend.minigame.wordle.dto.WordleDto.GameState;
import com.nueetemoins.backend.minigame.wordle.dto.WordleDto.GuessRequest;
import com.nueetemoins.backend.minigame.wordle.dto.WordleDto.LeaderboardEntry;
import com.nueetemoins.backend.profile.ProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/minigames/wordle")
@RequiredArgsConstructor
public class WordleController {
    private final WordleService service;
    private final ProfileService profileService;

    @GetMapping("/today")
    public GameState today(@RequestAttribute("userId") String userId) {
        return service.today(userId, profileService.require(userId).language());
    }

    @PostMapping("/guess")
    public GameState guess(@RequestAttribute("userId") String userId,
                        @Valid @RequestBody GuessRequest body) {
        return service.guess(userId, profileService.require(userId).language(), body.guess());
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntry> leaderboard(@RequestAttribute("userId") String userId) {
        return service.leaderboard(userId);
    }
}