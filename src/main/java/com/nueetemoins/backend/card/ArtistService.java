package com.nueetemoins.backend.card;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nueetemoins.backend.card.dto.ArtistRequest;
import com.nueetemoins.backend.card.dto.ArtistResponse;
import com.nueetemoins.backend.common.PublicIdGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final CardArtRepository cardArtRepository;

    @Transactional(readOnly = true)
    public List<ArtistResponse> findAll() {
        return artistRepository.findAll().stream().map(ArtistResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ArtistResponse findOne(String publicId) {
        return ArtistResponse.from(getOrThrow(publicId));
    }

    @Transactional
    public ArtistResponse create(ArtistRequest req) {
        Artist a = new Artist();
        String id;
        do {
            id = PublicIdGenerator.generate(6);
        } while (artistRepository.existsByPublicId(id));
        a.setPublicId(id);
        fill(a, req);
        return ArtistResponse.from(artistRepository.save(a));
    }

    @Transactional
    public ArtistResponse update(String publicId, ArtistRequest req) {
        Artist a = getOrThrow(publicId);
        fill(a, req);
        return ArtistResponse.from(a);
    }

    @Transactional
    public void delete(String publicId) {
        Artist a = getOrThrow(publicId);
        if (cardArtRepository.existsByArtistId(a.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Artist still has arts");
        }
        artistRepository.delete(a);
    }

    private void fill(Artist a, ArtistRequest req) {
        a.setName(req.name());
        a.setBio(req.bio());
        a.setUrl(req.url());
    }

    private Artist getOrThrow(String publicId) {
        return artistRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artist not found"));
    }
}
