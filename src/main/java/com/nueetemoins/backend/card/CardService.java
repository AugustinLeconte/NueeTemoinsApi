package com.nueetemoins.backend.card;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.nueetemoins.backend.card.dto.CardRequest;
import com.nueetemoins.backend.card.dto.CardResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;

    public List<CardResponse> findAll() {
        return cardRepository.findAll().stream().map(CardResponse::from).toList();
    }

    public CardResponse findById(UUID id) {
        return CardResponse.from(getOrThrow(id));
    }

    @Transactional
    public CardResponse create(CardRequest req) {
        Card card = new Card();
        apply(card, req);
        return CardResponse.from(cardRepository.save(card));
    }

    @Transactional
    public CardResponse update(UUID id, CardRequest req) {
        Card card = getOrThrow(id);
        apply(card, req);
        return CardResponse.from(card);
    }

    @Transactional
    public void delete(UUID id) {
        cardRepository.delete(getOrThrow(id));
    }

    private Card getOrThrow(UUID id) {
        return cardRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
    }

    private void apply(Card card, CardRequest req) {
        card.setFamily(req.family());
        card.setRarity(req.rarity());
        card.setTitle(req.title());
        card.setSubtitle(req.subtitle());
        card.setDescription(req.description());
        card.setImageUrl(req.imageUrl());
    }
}