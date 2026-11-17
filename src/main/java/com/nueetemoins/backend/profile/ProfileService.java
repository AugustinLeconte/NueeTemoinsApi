package com.nueetemoins.backend.profile;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
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

    /** userId -> pseudo, en une seule requête. */
    public Map<String, String> pseudosOf(Collection<String> userIds) {
        if (userIds.isEmpty()) return Map.of();
        String placeholders = String.join(",", Collections.nCopies(userIds.size(), "?"));
        Map<String, String> out = new HashMap<>();
        jdbc.query("SELECT user_id, pseudo FROM profiles WHERE user_id IN (" + placeholders + ")",
            (RowCallbackHandler) rs -> out.put(rs.getString("user_id"), rs.getString("pseudo")),
            userIds.toArray());
        return out;
    }
}
