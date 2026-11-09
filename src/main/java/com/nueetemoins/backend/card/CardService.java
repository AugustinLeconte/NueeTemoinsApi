package com.nueetemoins.backend.card;

import java.time.DateTimeException;
import java.time.MonthDay;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.nueetemoins.backend.card.dto.CardRequest;
import com.nueetemoins.backend.card.dto.CardResponse;
import com.nueetemoins.backend.card.dto.FeastDateRequest;
import com.nueetemoins.backend.card.dto.TranslationRequest;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CardService {

    private static final String DEFAULT_LOCALE = "fr";
    private static final String DEFAULT_COUNTRY = "FR";

    private final CardRepository cardRepository;

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
        if (cardRepository.existsByPublicId(req.publicId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "publicId already exists");
        }
        Card card = new Card();
        card.setPublicId(req.publicId());
        card.setFamily(req.family());
        card.setRarity(req.rarity());
        card.setImageUrl(req.imageUrl());

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

    private CardResponse toResponse(Card c, String lang, String country) {
        CardTranslation t = c.findTranslation(lang)
            .or(() -> c.findTranslation(DEFAULT_LOCALE))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "No translation for " + c.getPublicId()));

        CardFeastDate f = c.findFeastDate(country)
            .or(() -> c.findFeastDate(DEFAULT_COUNTRY))
            .orElse(null);

        return new CardResponse(
            c.getPublicId(), c.getFamily(), c.getRarity(), c.getImageUrl(),
            t.getLocale(), t.getTitle(), t.getSubtitle(), t.getDescription(),
            f != null ? f.getCountryCode() : null,
            f != null ? f.getMonth() : null,
            f != null ? f.getDay() : null);
    }

    private Card getOrThrow(String publicId) {
        return cardRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
    }

    private static String normLocale(String l)  { return (l == null || l.isBlank()) ? DEFAULT_LOCALE  : l.trim().toLowerCase(); }
    private static String normCountry(String c) { return (c == null || c.isBlank()) ? DEFAULT_COUNTRY : c.trim().toUpperCase(); }
}