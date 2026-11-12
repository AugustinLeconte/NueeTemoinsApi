package com.nueetemoins.backend.family;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nueetemoins.backend.card.CardRepository;
import com.nueetemoins.backend.common.PublicIdGenerator;
import com.nueetemoins.backend.family.dto.FamilyAdminResponse;
import com.nueetemoins.backend.family.dto.FamilyRequest;
import com.nueetemoins.backend.family.dto.FamilyResponse;
import com.nueetemoins.backend.family.dto.FamilyTranslationRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FamilyService {

    private static final String DEFAULT_LOCALE = "fr";
    private final FamilyRepository familyRepository;
    private final CardRepository cardRepository;

    @Transactional(readOnly = true)
    public List<FamilyResponse> findAll(String lang) {
        String l = normLocale(lang);
        return familyRepository.findAll().stream().map(f -> toResponse(f, l)).toList();
    }

    @Transactional
    public FamilyResponse create(FamilyRequest req, String lang) {
        String publicId = newPublicId();
        if (familyRepository.existsByPublicId(publicId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "publicId already exists");
        }
        Family f = new Family();
        f.setPublicId(publicId);
        req.translations().forEach(t -> upsertTranslation(f, t.locale(), t.name()));
        if (f.findTranslation(DEFAULT_LOCALE).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A '" + DEFAULT_LOCALE + "' translation is required");
        }
        return toResponse(familyRepository.save(f), normLocale(lang));
    }

    @Transactional
    public void putTranslation(String publicId, String locale, FamilyTranslationRequest req) {
        Family f = getOrThrow(publicId);
        upsertTranslation(f, locale, req.name());
    }

    @Transactional(readOnly = true)
    public List<FamilyAdminResponse> findAllAdmin() {
        return familyRepository.findAll().stream()
            .map(f -> new FamilyAdminResponse(f.getPublicId(),
                f.getTranslations().stream()
                    .map(t -> new FamilyTranslationRequest(t.getLocale(), t.getName()))
                    .toList()))
            .toList();
    }

    @Transactional
    public void update(String publicId, FamilyRequest req) {
        Family f = getOrThrow(publicId);
        Set<String> locales = req.translations().stream()
            .map(t -> normLocale(t.locale())).collect(Collectors.toSet());
        if (!locales.contains(DEFAULT_LOCALE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A '" + DEFAULT_LOCALE + "' translation is required");
        }
        f.getTranslations().removeIf(t -> !locales.contains(t.getLocale()));
        req.translations().forEach(t -> upsertTranslation(f, t.locale(), t.name()));
    }

    @Transactional
    public void delete(String publicId) {
        Family f = getOrThrow(publicId);
        if (cardRepository.existsByFamiliesContaining(f)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Family is used by cards");
        }
        familyRepository.delete(f);
    }

    @Transactional(readOnly = true)
    public List<Family> resolveAll(Collection<String> publicIds) {
        List<Family> found = familyRepository.findByPublicIdIn(publicIds);
        if (found.size() != new HashSet<>(publicIds).size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown family");
        }
        return found;
    }

    public FamilyResponse toResponse(Family f, String lang) {
        String name = f.findTranslation(lang)
            .or(() -> f.findTranslation(DEFAULT_LOCALE))
            .map(t -> t.getName())
            .orElse(f.getPublicId());
        return new FamilyResponse(f.getPublicId(), name);
    }

    private void upsertTranslation(Family f, String rawLocale, String name) {
        String locale = normLocale(rawLocale);
        FamilyTranslation t = f.findTranslation(locale).orElseGet(() -> {
            FamilyTranslation n = new FamilyTranslation();
            n.setFamily(f);
            n.setLocale(locale);
            f.getTranslations().add(n);
            return n;
        });
        t.setName(name);
    }

    private Family getOrThrow(String publicId) {
        return familyRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Family not found"));
    }

    private static String normLocale(String l) {
        return (l == null || l.isBlank()) ? DEFAULT_LOCALE : l.trim().toLowerCase();
    }

    private String newPublicId() {
        String id;
        do {
            id = PublicIdGenerator.generate(6);
        } while (cardRepository.existsByPublicId(id));
        return id;
    }
}