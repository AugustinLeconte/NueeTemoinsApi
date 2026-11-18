package com.nueetemoins.backend.minigame.guesscard;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nueetemoins.backend.minigame.MiniGameResultService;
import com.nueetemoins.backend.minigame.MiniGameResultService.LeaderboardRow;
import com.nueetemoins.backend.minigame.MiniGameType;
import com.nueetemoins.backend.minigame.guesscard.GuessCardService.CardName;
import com.nueetemoins.backend.minigame.guesscard.GuessCardService.StateView;
import com.nueetemoins.backend.profile.ProfileService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/minigames/guess-card")
@RequiredArgsConstructor
public class GuessCardController {

    private final GuessCardService service;
    private final MiniGameResultService results;
    private final ProfileService profileService;

    public record GuessRequest(@NotBlank String cardId) {}

    /** Langue du profil (comme le Wordle) : on ignore tout paramètre ?lang= envoyé par le front. */
    private String lang(String userId) { return profileService.require(userId).language(); }

    @GetMapping("/today")
    public StateView today(@RequestAttribute("userId") String userId) {
        return service.today(userId, lang(userId));
    }

    @PostMapping("/guess")
    public StateView guess(@RequestAttribute("userId") String userId, @Valid @RequestBody GuessRequest body) {
        return service.guess(userId, body.cardId(), lang(userId));
    }

    @GetMapping("/cards")
    public List<CardName> cards(@RequestAttribute("userId") String userId) {
        return service.names(lang(userId));
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardRow> leaderboard(@RequestAttribute("userId") String userId) {
        return results.leaderboard(userId, MiniGameType.GUESS_CARD);
    }
}