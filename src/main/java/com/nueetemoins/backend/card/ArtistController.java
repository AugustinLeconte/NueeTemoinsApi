package com.nueetemoins.backend.card;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nueetemoins.backend.card.dto.ArtistRequest;
import com.nueetemoins.backend.card.dto.ArtistResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;

    @GetMapping
    public List<ArtistResponse> getAll() { return artistService.findAll(); }

    @GetMapping("/{publicId}")
    public ArtistResponse getOne(@PathVariable String publicId) { return artistService.findOne(publicId); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArtistResponse create(@Valid @RequestBody ArtistRequest request) { return artistService.create(request); }

    @PutMapping("/{publicId}")
    public ArtistResponse update(@PathVariable String publicId, @Valid @RequestBody ArtistRequest request) {
        return artistService.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String publicId) { artistService.delete(publicId); }
}
