package com.nueetemoins.backend.card;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.nueetemoins.backend.card.dto.ArtTranslationRequest;
import com.nueetemoins.backend.card.dto.ArtistResponse;
import com.nueetemoins.backend.card.dto.CardArtRequest;
import com.nueetemoins.backend.card.dto.CardArtResponse;
import com.nueetemoins.backend.common.PublicIdGenerator;
import com.nueetemoins.backend.image.ImageStorageService;
import com.nueetemoins.backend.player.PlayerArtRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CardArtService {

    private static final String DEFAULT_LOCALE = "fr";

    private final CardArtRepository artRepository;
    private final CardRepository cardRepository;
    private final ArtistRepository artistRepository;
    private final PlayerArtRepository playerArtRepository;
    private final ImageStorageService imageStorage;

    @Transactional(readOnly = true)
    public List<CardArtResponse> findByCard(String cardPublicId, String lang) {
        Card card = getCardOrThrow(cardPublicId);
        return card.getArts().stream().map(a -> toResponse(a, lang)).toList();
    }

    @Transactional(readOnly = true)
    public CardArtResponse findOne(String artPublicId, String lang) {
        return toResponse(getOrThrow(artPublicId), lang);
    }

    @Transactional(readOnly = true)
    public CardArtRequest findAdmin(String artPublicId) {
        CardArt a = getOrThrow(artPublicId);
        return new CardArtRequest(
            a.getArtist() == null ? null : a.getArtist().getPublicId(),
            a.getRarity(), a.getSource(), a.getImageUrl(),
            a.getDropWeight(), a.getCoinPrice(), a.isObtainable(),
            a.getTranslations().stream()
                .map(t -> new ArtTranslationRequest(t.getLocale(), t.getTitle(), t.getDescription()))
                .toList());
    }

    @Transactional
    public CardArtResponse create(String cardPublicId, CardArtRequest req, String lang) {
        Card card = getCardOrThrow(cardPublicId);
        CardArt art = new CardArt();
        art.setPublicId(newPublicId());
        art.setCard(card);
        apply(art, req);
        return toResponse(artRepository.save(art), lang);
    }

    @Transactional
    public CardArtResponse update(String artPublicId, CardArtRequest req, String lang) {
        CardArt art = getOrThrow(artPublicId);
        String oldUrl = art.getImageUrl();
        apply(art, req);
        if (!Objects.equals(oldUrl, art.getImageUrl())) {
            deleteImageAfterCommit(oldUrl);
        }
        return toResponse(art, lang);
    }

    @Transactional
    public void delete(String artPublicId) {
        CardArt art = getOrThrow(artPublicId);
        if (art.isDefault()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The default art cannot be deleted");
        }
        if (playerArtRepository.existsByArtId(art.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Art is owned by players: set obtainable=false instead");
        }
        String url = art.getImageUrl();
        art.getCard().getArts().remove(art); // orphanRemoval s'occupe du DELETE
        deleteImageAfterCommit(url);
    }

    /** À appeler dans une transaction (accès lazy). */
    public CardArtResponse toResponse(CardArt a, String lang) {
        String l = normLocale(lang);
        CardArtTranslation t = a.findTranslation(l)
            .or(() -> a.findTranslation(DEFAULT_LOCALE)).orElse(null);
        Card card = a.getCard();
        String cardTitle = card.findTranslation(l)
            .or(() -> card.findTranslation(DEFAULT_LOCALE))
            .map(CardTranslation::getTitle).orElse(null);
        Artist artist = a.getArtist();
        return new CardArtResponse(
            a.getPublicId(), card.getPublicId(), cardTitle,
            artist == null ? null : ArtistResponse.from(artist),
            a.getRarity(), a.getSource(), a.getImageUrl(), a.getCoinPrice(),
            t == null ? null : t.getTitle(),
            t == null ? null : t.getDescription());
    }

    private void apply(CardArt art, CardArtRequest req) {
        if (req.source() == ArtSource.SHOP && req.coinPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "coinPrice is required for SHOP arts");
        }
        if (req.source() == ArtSource.DEFAULT) {
            if (req.rarity() != CardRarity.COMMON) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The default art must be COMMON");
            }
            artRepository.findByCardIdAndSource(art.getCard().getId(), ArtSource.DEFAULT)
                .filter(other -> !Objects.equals(other.getId(), art.getId()))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "This card already has a default art");
                });
        }

        if (req.artistPublicId() == null || req.artistPublicId().isBlank()) {
            art.setArtist(null);
        } else {
            art.setArtist(artistRepository.findByPublicId(req.artistPublicId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown artist")));
        }

        art.setRarity(req.rarity());
        art.setSource(req.source());
        art.setImageUrl(req.imageUrl());
        art.setDropWeight(req.dropWeight() == null ? 100 : req.dropWeight());
        art.setCoinPrice(req.source() == ArtSource.SHOP ? req.coinPrice() : null);
        art.setObtainable(req.obtainable() == null || req.obtainable());

        List<ArtTranslationRequest> trs = req.translations() == null ? List.of() : req.translations();
        Set<String> locales = trs.stream().map(t -> normLocale(t.locale())).collect(Collectors.toSet());
        art.getTranslations().removeIf(t -> !locales.contains(t.getLocale()));
        trs.forEach(t -> upsertTranslation(art, t));
    }

    private void upsertTranslation(CardArt art, ArtTranslationRequest req) {
        String locale = normLocale(req.locale());
        CardArtTranslation t = art.findTranslation(locale).orElseGet(() -> {
            CardArtTranslation n = new CardArtTranslation();
            n.setArt(art);
            n.setLocale(locale);
            art.getTranslations().add(n);
            return n;
        });
        t.setTitle(req.title());
        t.setDescription(req.description());
    }

    private Card getCardOrThrow(String publicId) {
        return cardRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
    }

    private CardArt getOrThrow(String publicId) {
        return artRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Art not found"));
    }

    private String newPublicId() {
        String id;
        do {
            id = PublicIdGenerator.generate(6);
        } while (artRepository.existsByPublicId(id));
        return id;
    }

    private void deleteImageAfterCommit(String url) {
        if (url == null) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                imageStorage.deleteByUrl(url);
            }
        });
    }

    private static String normLocale(String l) {
        return (l == null || l.isBlank()) ? DEFAULT_LOCALE : l.trim().toLowerCase();
    }
}
