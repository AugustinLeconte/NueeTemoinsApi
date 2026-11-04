package com.nueetemoins.backend.card;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nueetemoins.backend.card.dto.CardRequest;
import com.nueetemoins.backend.card.dto.CardResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @GetMapping
    public List<CardResponse> getAll() {
        return cardService.findAll();
    }

    @PostMapping
    public CardResponse create(@Valid @RequestBody CardRequest request) {
        return cardService.create(request);
    }
}
    