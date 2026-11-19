package com.nueetemoins.backend.card;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nueetemoins.backend.minigame.DailyChallengeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CardNameService {

    public record CardName(String id, String name) {}
    public record CardSummary(String id, String name, String imageUrl) {}

    private final CardRepository cardRepository;

    @Transactional(readOnly = true)
    public List<CardName> names(String lang) {
        return cardRepository.findAll().stream()
            .map(c -> new CardName(c.getPublicId(), DailyChallengeService.name(c, lang)))
            .sorted(Comparator.comparing(CardName::name, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Transactional(readOnly = true)
    public boolean exists(String publicId) {
        return cardRepository.existsByPublicId(publicId);
    }

    @Transactional(readOnly = true)
    public String nameOf(String publicId, String lang) {
        return cardRepository.findByPublicId(publicId)
            .map(c -> DailyChallengeService.name(c, lang))
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<CardSummary> summaryOf(String publicId, String lang) {
        return cardRepository.findByPublicId(publicId).map(c -> new CardSummary(
            c.getPublicId(),
            DailyChallengeService.name(c, lang),
            c.findDefaultArt().map(CardArt::getImageUrl).orElse(null)));
    }
}