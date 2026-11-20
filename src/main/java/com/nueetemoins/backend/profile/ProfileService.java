package com.nueetemoins.backend.profile;

import java.sql.ResultSet;
import java.sql.SQLException;
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

import com.nueetemoins.backend.card.CardNameService;

import lombok.RequiredArgsConstructor;

/** Lecture du profil pour les services joueur (pays + langue + saint patron). */
@Service
@RequiredArgsConstructor
public class ProfileService {
    public static final String TERMS_VERSION = "2026-11-20";

    private final JdbcTemplate jdbc;
    private final CardNameService cards;

    public record Info(String pseudo, String country, String language,
                    String patronCardId, boolean termsAccepted) {}
    public record Found(String userId, Info info) {}

    private static Info toInfo(ResultSet rs) throws SQLException {
        return new Info(rs.getString("pseudo"), rs.getString("country"),
                        rs.getString("language"), rs.getString("patron_card_id"),
                        TERMS_VERSION.equals(rs.getString("terms_accepted_version")));
    }

    public Info require(String userId) {
        List<Info> rows = jdbc.query(
            "SELECT pseudo, country, language, patron_card_id, terms_accepted_version FROM profiles WHERE user_id = ?",
            (rs, i) -> toInfo(rs), userId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
        return rows.get(0);
    }

    public Found requireByPseudo(String pseudo) {
        List<Found> rows = jdbc.query(
            "SELECT user_id, pseudo, country, language, patron_card_id, terms_accepted_version FROM profiles WHERE pseudo = ?",
            (rs, i) -> new Found(rs.getString("user_id"), toInfo(rs)),
            pseudo);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found");
        return rows.get(0);
    }

    public void acceptTerms(String userId) {
        int updated = jdbc.update(
            "UPDATE profiles SET terms_accepted_version = ?, terms_accepted_at = CURRENT_TIMESTAMP WHERE user_id = ?",
            TERMS_VERSION, userId);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
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

    /** cardId null ou vide = retirer le saint patron. */
    public void setPatron(String userId, String cardId) {
        String id = (cardId == null || cardId.isBlank()) ? null : cardId;
        if (id != null && !cards.exists(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown card");
        }
        int updated = jdbc.update("UPDATE profiles SET patron_card_id = ? WHERE user_id = ?", id, userId);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
    }
}