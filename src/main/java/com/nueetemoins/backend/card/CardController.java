package com.nueetemoins.backend.card;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nueetemoins.backend.card.dto.CardRequest;
import com.nueetemoins.backend.card.dto.CardResponse;
import com.nueetemoins.backend.card.dto.FeastDateRequest;
import com.nueetemoins.backend.card.dto.TranslationRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @GetMapping
    public List<CardResponse> getAll(@RequestParam(required = false) String lang,
                                     @RequestParam(required = false) String country) {
        return cardService.findAll(lang, country);
    }

    @GetMapping("/{publicId}")
    public CardResponse getOne(@PathVariable String publicId,
                               @RequestParam(required = false) String lang,
                               @RequestParam(required = false) String country) {
        return cardService.findByPublicId(publicId, lang, country);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse create(@Valid @RequestBody CardRequest request,
                               @RequestParam(required = false) String lang,
                               @RequestParam(required = false) String country) {
        return cardService.create(request, lang, country);
    }

    @PutMapping("/{publicId}/translations/{locale}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putTranslation(@PathVariable String publicId, @PathVariable String locale,
                               @Valid @RequestBody TranslationRequest request) {
        cardService.putTranslation(publicId, locale, request);
    }

    @PutMapping("/{publicId}/feast-dates/{countryCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putFeastDate(@PathVariable String publicId, @PathVariable String countryCode,
                             @Valid @RequestBody FeastDateRequest request) {
        cardService.putFeastDate(publicId, countryCode, request);
    }
}
    