package com.nueetemoins.backend.shop;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nueetemoins.backend.player.dto.WalletResponse;
import com.nueetemoins.backend.profile.ProfileService;
import com.nueetemoins.backend.shop.dto.FreeOffersResponse;
import com.nueetemoins.backend.shop.dto.ShopItemResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final ShopRewardService rewardService;
    private final ProfileService profileService;

    @GetMapping
    public List<ShopItemResponse> shop(@RequestAttribute("userId") String userId) {
        var p = profileService.require(userId);
        return shopService.shop(userId, p.language());
    }

    @PostMapping("/arts/{artPublicId}/buy")
    public WalletResponse buy(@RequestAttribute("userId") String userId,
                              @PathVariable String artPublicId) {
        profileService.require(userId);
        return shopService.buy(userId, artPublicId);
    }

    @GetMapping("/free")
    public FreeOffersResponse freeOffers(@RequestAttribute("userId") String userId) {
        profileService.require(userId);
        return rewardService.offers(userId);
    }

    @PostMapping("/free/daily")
    public WalletResponse claimDaily(@RequestAttribute("userId") String userId) {
        profileService.require(userId);
        return rewardService.claimDaily(userId);
    }

    @PostMapping("/free/ad")
    public WalletResponse claimAd(@RequestAttribute("userId") String userId) {
        profileService.require(userId);
        return rewardService.claimAd(userId);
    }
}