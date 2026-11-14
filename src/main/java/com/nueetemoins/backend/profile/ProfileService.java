package com.nueetemoins.backend.profile;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

/** Lecture du profil pour les services joueur (pays + langue). */
@Service
@RequiredArgsConstructor
public class ProfileService {

    public record Info(String pseudo, String country, String language) {}

    private final JdbcTemplate jdbc;

    public Info require(String userId) {
        List<Info> rows = jdbc.query(
            "SELECT pseudo, country, language FROM profiles WHERE user_id = ?",
            (rs, i) -> new Info(rs.getString("pseudo"), rs.getString("country"), rs.getString("language")),
            userId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
        }
        return rows.get(0);
    }
}
