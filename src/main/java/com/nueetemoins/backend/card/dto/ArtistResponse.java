package com.nueetemoins.backend.card.dto;

import com.nueetemoins.backend.card.Artist;

public record ArtistResponse(String publicId, String name, String bio, String url) {
    public static ArtistResponse from(Artist a) {
        return new ArtistResponse(a.getPublicId(), a.getName(), a.getBio(), a.getUrl());
    }
}
