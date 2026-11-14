package com.nueetemoins.backend.card;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nueetemoins.backend.card.dto.CardArtRequest;
import com.nueetemoins.backend.card.dto.CardArtResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CardArtController {

    private final CardArtService artService;

    @GetMapping("/api/cards/{cardPublicId}/arts")
    public List<CardArtResponse> getByCard(@PathVariable String cardPublicId,
                                           @RequestParam(required = false) String lang) {
        return artService.findByCard(cardPublicId, lang);
    }

    @PostMapping("/api/cards/{cardPublicId}/arts")
    @ResponseStatus(HttpStatus.CREATED)
    public CardArtResponse create(@PathVariable String cardPublicId,
                                  @Valid @RequestBody CardArtRequest request,
                                  @RequestParam(required = false) String lang) {
        return artService.create(cardPublicId, request, lang);
    }

    @GetMapping("/api/arts/{artPublicId}")
    public CardArtResponse getOne(@PathVariable String artPublicId,
                                  @RequestParam(required = false) String lang) {
        return artService.findOne(artPublicId, lang);
    }

    @GetMapping("/api/arts/{artPublicId}/admin")
    public CardArtRequest getAdmin(@PathVariable String artPublicId) {
        return artService.findAdmin(artPublicId);
    }

    @PutMapping("/api/arts/{artPublicId}")
    public CardArtResponse update(@PathVariable String artPublicId,
                                  @Valid @RequestBody CardArtRequest request,
                                  @RequestParam(required = false) String lang) {
        return artService.update(artPublicId, request, lang);
    }

    @DeleteMapping("/api/arts/{artPublicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String artPublicId) { artService.delete(artPublicId); }
}
