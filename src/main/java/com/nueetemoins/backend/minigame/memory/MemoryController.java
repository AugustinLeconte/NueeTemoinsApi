package com.nueetemoins.backend.minigame.memory;

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
import com.nueetemoins.backend.minigame.memory.MemoryService.FlipResult;
import com.nueetemoins.backend.minigame.memory.MemoryService.MemoryState;
import com.nueetemoins.backend.profile.ProfileService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/minigames/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService service;
    private final MiniGameResultService results;
    private final ProfileService profileService;

    public record FlipRequest(@Min(0) int index) {}

    private String lang(String userId) { return profileService.require(userId).language(); }

    @GetMapping("/today")
    public MemoryState today(@RequestAttribute("userId") String userId) {
        return service.today(userId, lang(userId));
    }

    @PostMapping("/flip")
    public FlipResult flip(@RequestAttribute("userId") String userId, @Valid @RequestBody FlipRequest body) {
        return service.flip(userId, body.index(), lang(userId));
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardRow> leaderboard(@RequestAttribute("userId") String userId) {
        return results.leaderboard(userId, MiniGameType.MEMORY);
    }
}