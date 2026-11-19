package com.nueetemoins.backend.profile;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nueetemoins.backend.achievement.AchievementService;
import com.nueetemoins.backend.card.CardNameService;
import com.nueetemoins.backend.player.GameRules;
import com.nueetemoins.backend.player.PlayerWallet;
import com.nueetemoins.backend.player.PlayerWalletRepository;
import com.nueetemoins.backend.profile.dto.PublicProfileResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicProfileService {

    private final ProfileService profiles;
    private final PlayerWalletRepository wallets;
    private final AchievementService achievements;
    private final CardNameService cards;

    @Transactional(readOnly = true)
    public PublicProfileResponse get(String pseudo, String lang) {
        var found = profiles.requireByPseudo(pseudo);
        var info = found.info();

        int xp = wallets.findById(found.userId()).map(PlayerWallet::getXp).orElse(0);

        // Patron supprimé entre-temps : on l'ignore plutôt que de planter.
        PublicProfileResponse.Patron patron = Optional.ofNullable(info.patronCardId())
            .flatMap(id -> cards.summaryOf(id, lang))
            .map(s -> new PublicProfileResponse.Patron(s.id(), s.name(), s.imageUrl()))
            .orElse(null);

        return new PublicProfileResponse(
            info.pseudo(), info.country(), GameRules.levelForXp(xp),
            patron, achievements.unlockedOf(found.userId()));
    }
}