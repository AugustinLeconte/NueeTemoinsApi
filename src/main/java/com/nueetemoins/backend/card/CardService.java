package com.nueetemoins.backend.card;

import java.time.DateTimeException;
import java.time.MonthDay;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.nueetemoins.backend.card.dto.CardAdminResponse;
import com.nueetemoins.backend.card.dto.CardArtResponse;
import com.nueetemoins.backend.card.dto.CardRequest;
import com.nueetemoins.backend.card.dto.CardResponse;
import com.nueetemoins.backend.card.dto.FeastDateRequest;
import com.nueetemoins.backend.card.dto.TranslationRequest;
import com.nueetemoins.backend.common.PublicIdGenerator;
import com.nueetemoins.backend.family.FamilyService;
import com.nueetemoins.backend.family.dto.FamilyResponse;
import com.nueetemoins.backend.image.ImageStorageService;
import com.nueetemoins.backend.player.PlayerCardRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CardService {

    private static final String DEFAULT_LOCALE = "fr";
    private static final String DEFAULT_COUNTRY = "FR";

    private final CardRepository cardRepository;
    private final FamilyService familyService;
    private final ImageStorageService imageStorage;
    private final CardArtService cardArtService;
    private final PlayerCardRepository playerCardRepository;

    @Transactional(readOnly = true)
    public List<CardResponse> findAll(String lang, String country) {
        return cardRepository.findAll().stream()
            .map(c -> toResponse(c, normLocale(lang), normCountry(country)))
            .toList();
    }

    @Transactional(readOnly = true)
    public CardResponse findByPublicId(String publicId, String lang, String country) {
        return toResponse(getOrThrow(publicId), normLocale(lang), normCountry(country));
    }

    @Transactional
    public CardResponse create(CardRequest req, String lang, String country) {
        Card card = new Card();
        card.setPublicId(newPublicId());
        card.getFamilies().addAll(familyService.resolveAll(req.families()));

        req.translations().forEach(t -> upsertTranslation(card, t));
        if (req.feastDates() != null) {
            req.feastDates().forEach(f -> upsertFeastDate(card, f));
        }
        if (card.findTranslation(DEFAULT_LOCALE).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A '" + DEFAULT_LOCALE + "' translation is required");
        }
        return toResponse(cardRepository.save(card), normLocale(lang), normCountry(country));
    }

    @Transactional
    public void putTranslation(String publicId, String locale, TranslationRequest req) {
        upsertTranslation(getOrThrow(publicId),
            new TranslationRequest(locale, req.title(), req.subtitle(), req.description()));
    }

    @Transactional
    public void putFeastDate(String publicId, String countryCode, FeastDateRequest req) {
        upsertFeastDate(getOrThrow(publicId),
            new FeastDateRequest(countryCode, req.month(), req.day()));
    }

    private void upsertTranslation(Card card, TranslationRequest req) {
        String locale = normLocale(req.locale());
        CardTranslation t = card.findTranslation(locale).orElseGet(() -> {
            CardTranslation n = new CardTranslation();
            n.setCard(card);
            n.setLocale(locale);
            card.getTranslations().add(n);
            return n;
        });
        t.setTitle(req.title());
        t.setSubtitle(req.subtitle());
        t.setDescription(req.description());
    }

    @Transactional
    public void putFamilies(String publicId, List<String> familyIds) {
        Card card = getOrThrow(publicId);
        card.getFamilies().clear();
        card.getFamilies().addAll(familyService.resolveAll(familyIds));
    }

    @Transactional(readOnly = true)
    public CardAdminResponse findAdmin(String publicId) {
        Card c = getOrThrow(publicId);
        return new CardAdminResponse(
            c.getPublicId(),
            c.getFamilies().stream().map(fam -> fam.getPublicId()).toList(),
            c.getTranslations().stream()
                .map(t -> new TranslationRequest(t.getLocale(), t.getTitle(), t.getSubtitle(), t.getDescription()))
                .toList(),
            c.getFeastDates().stream()
                .map(fd -> new FeastDateRequest(fd.getCountryCode(), fd.getMonth(), fd.getDay()))
                .toList());
    }

    @Transactional
    public CardResponse update(String publicId, CardRequest req, String lang, String country) {
        Card card = getOrThrow(publicId);

        card.getFamilies().clear();
        card.getFamilies().addAll(familyService.resolveAll(req.families()));

        Set<String> locales = req.translations().stream()
            .map(t -> normLocale(t.locale())).collect(Collectors.toSet());
        if (!locales.contains(DEFAULT_LOCALE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A '" + DEFAULT_LOCALE + "' translation is required");
        }
        card.getTranslations().removeIf(t -> !locales.contains(t.getLocale()));
        req.translations().forEach(t -> upsertTranslation(card, t));

        List<FeastDateRequest> feasts = req.feastDates() == null ? List.of() : req.feastDates();
        Set<String> countries = feasts.stream()
            .map(fd -> normCountry(fd.countryCode())).collect(Collectors.toSet());
        card.getFeastDates().removeIf(fd -> !countries.contains(fd.getCountryCode()));
        feasts.forEach(fd -> upsertFeastDate(card, fd));

        return toResponse(card, normLocale(lang), normCountry(country));
    }

    @Transactional
    public void delete(String publicId) {
        Card card = getOrThrow(publicId);
        if (playerCardRepository.existsByCardId(card.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Card is owned by players");
        }
        List<String> urls = card.getArts().stream().map(CardArt::getImageUrl).toList();
        cardRepository.delete(card);
        urls.forEach(this::deleteImageAfterCommit);
    }

    private void upsertFeastDate(Card card, FeastDateRequest req) {
        try {
            MonthDay.of(req.month(), req.day());
        } catch (DateTimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid feast date");
        }
        String country = normCountry(req.countryCode());
        CardFeastDate f = card.findFeastDate(country).orElseGet(() -> {
            CardFeastDate n = new CardFeastDate();
            n.setCard(card);
            n.setCountryCode(country);
            card.getFeastDates().add(n);
            return n;
        });
        f.setMonth(req.month());
        f.setDay(req.day());
    }

    /** Public : réutilisé par le côté joueur. À appeler dans une transaction. */
    public CardResponse toResponse(Card c, String lang, String country) {
        CardTranslation t = c.findTranslation(lang)
            .or(() -> c.findTranslation(DEFAULT_LOCALE))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "No translation for " + c.getPublicId()));

        CardFeastDate feastDate = c.findFeastDate(country)
            .or(() -> c.findFeastDate(DEFAULT_COUNTRY))
            .orElse(null);

        List<FamilyResponse> families = c.getFamilies().stream()
            .map(family -> familyService.toResponse(family, lang))
            .toList();

        CardArtResponse defaultArt = c.findDefaultArt()
            .map(a -> cardArtService.toResponse(a, lang))
            .orElse(null);

        return new CardResponse(
            c.getPublicId(), families,
            t.getLocale(), t.getTitle(), t.getSubtitle(), t.getDescription(),
            feastDate != null ? feastDate.getCountryCode() : null,
            feastDate != null ? feastDate.getMonth() : null,
            feastDate != null ? feastDate.getDay() : null,
            defaultArt);
    }

    private Card getOrThrow(String publicId) {
        return cardRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
    }

    private String newPublicId() {
        String id;
        do {
            id = PublicIdGenerator.generate(6);
        } while (cardRepository.existsByPublicId(id));
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

    private static String normLocale(String l)  { return (l == null || l.isBlank()) ? DEFAULT_LOCALE  : l.trim().toLowerCase(); }
    private static String normCountry(String c) { return (c == null || c.isBlank()) ? DEFAULT_COUNTRY : c.trim().toUpperCase(); }
}
