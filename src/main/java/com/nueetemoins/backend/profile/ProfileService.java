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

import com.nueetemoins.backend.card.CardNameService;

import lombok.RequiredArgsConstructor;

/** Lecture du profil pour les services joueur (pays + langue + saint patron). */
@Service
@RequiredArgsConstructor
public class ProfileService {

    public record Info(String pseudo, String country, String language, String patronCardId) {}
    public record Found(String userId, Info info) {}

    private final JdbcTemplate jdbc;
    private final CardNameService cards;

    public Info require(String userId) {
        List<Info> rows = jdbc.query(
            "SELECT pseudo, country, language, patron_card_id FROM profiles WHERE user_id = ?",
            (rs, i) -> new Info(rs.getString("pseudo"), rs.getString("country"),
                                rs.getString("language"), rs.getString("patron_card_id")),
            userId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
        return rows.get(0);
    }

    public Found requireByPseudo(String pseudo) {
        List<Found> rows = jdbc.query(
            "SELECT user_id, pseudo, country, language, patron_card_id FROM profiles WHERE pseudo = ?",
            (rs, i) -> new Found(rs.getString("user_id"),
                new Info(rs.getString("pseudo"), rs.getString("country"),
                         rs.getString("language"), rs.getString("patron_card_id"))),
            pseudo);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found");
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