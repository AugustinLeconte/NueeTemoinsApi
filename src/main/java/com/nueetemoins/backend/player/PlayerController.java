package com.nueetemoins.backend.player;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nueetemoins.backend.achievement.dto.AchievementResponse;
import com.nueetemoins.backend.player.dto.EquipRequest;
import com.nueetemoins.backend.player.dto.PackOfferResponse;
import com.nueetemoins.backend.player.dto.PackResult;
import com.nueetemoins.backend.player.dto.PlayerCardResponse;
import com.nueetemoins.backend.player.dto.ProgressResponse;
import com.nueetemoins.backend.player.dto.ShopItemResponse;
import com.nueetemoins.backend.player.dto.UpgradeResponse;
import com.nueetemoins.backend.player.dto.WalletResponse;
import com.nueetemoins.backend.profile.ProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Tout ce qui concerne le joueur connecté. Langue et pays viennent de son profil. */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerCollectionService collectionService;
    private final PackService packService;
    private final ProfileService profileService;
    private final PlayerProgressService progressService;

    @GetMapping("/wallet")
    public WalletResponse wallet(@RequestAttribute("userId") String userId) {
        profileService.require(userId);
        return collectionService.wallet(userId);
    }

    @GetMapping("/collection")
    public List<PlayerCardResponse> collection(@RequestAttribute("userId") String userId) {
        var p = profileService.require(userId);
        return collectionService.collection(userId, p.language(), p.country());
    }

    @PutMapping("/cards/{cardPublicId}/equipped-art")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void equip(@RequestAttribute("userId") String userId,
                      @PathVariable String cardPublicId,
                      @Valid @RequestBody EquipRequest request) {
        collectionService.equip(userId, cardPublicId, request.artPublicId());
    }

    @PostMapping("/cards/{cardPublicId}/upgrade")
    public UpgradeResponse upgrade(@RequestAttribute("userId") String userId,
                                   @PathVariable String cardPublicId) {
        return collectionService.upgrade(userId, cardPublicId);
    }

    @PostMapping("/packs/daily")
    public PackResult openDaily(@RequestAttribute("userId") String userId) {
        var p = profileService.require(userId);
        return packService.openDaily(userId, p.language(), p.country());
    }

    @GetMapping("/shop")
    public List<ShopItemResponse> shop(@RequestAttribute("userId") String userId) {
        var p = profileService.require(userId);
        return collectionService.shop(userId, p.language());
    }

    @PostMapping("/shop/arts/{artPublicId}/buy")
    public WalletResponse buy(@RequestAttribute("userId") String userId,
                              @PathVariable String artPublicId) {
        profileService.require(userId);
        return collectionService.buy(userId, artPublicId);
    }

    // ---------- Niveau du joueur & packs de niveau ----------

    @GetMapping("/progress")
    public ProgressResponse progress(@RequestAttribute("userId") String userId) {
        profileService.require(userId);
        return progressService.progress(userId);
    }

    @GetMapping("/shop/packs")
    public List<PackOfferResponse> packOffers(@RequestAttribute("userId") String userId) {
        profileService.require(userId);
        return progressService.packOffers(userId);
    }

    @PostMapping("/shop/packs/{level}/open")
    public PackResult openLevelPack(@RequestAttribute("userId") String userId, @PathVariable int level) {
        var p = profileService.require(userId);
        return progressService.openLevelPack(userId, level, p.language(), p.country());
    }

    @GetMapping("/achievements")
    public List<AchievementResponse> achievements(@RequestAttribute("userId") String userId) {
        profileService.require(userId);
        return progressService.achievements(userId);
    }
}
