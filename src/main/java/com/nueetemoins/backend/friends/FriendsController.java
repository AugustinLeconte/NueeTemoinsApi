package com.nueetemoins.backend.friends;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendsController {

    private final JdbcTemplate jdbc;

    private static final String NORM_PSEUDO =
    "LOWER(TRIM(REGEXP_REPLACE(TRANSLATE(pseudo, chr(160), ' '), ' +', ' ', 'g')))";

    private static String normalize(String s) {
        return s == null ? "" : s.replaceAll("[\\s\\u00A0]+", " ").trim();
    }

    public record FriendDto(long id, String pseudo, String country) {}
    public record Overview(List<FriendDto> friends, List<FriendDto> incoming, List<FriendDto> outgoing) {}
    public record AddRequest(String pseudo) {}

    /** Amis + demandes reçues + demandes envoyées, en une requête. */
    @GetMapping
    public Overview overview(@RequestAttribute("userId") String userId) {
        List<FriendDto> friends = new ArrayList<>();
        List<FriendDto> incoming = new ArrayList<>();
        List<FriendDto> outgoing = new ArrayList<>();

        jdbc.query("""
            SELECT f.id, f.status, f.requester_id, p.pseudo, p.country
            FROM friendships f
            JOIN profiles p
              ON p.user_id = CASE WHEN f.requester_id = ? THEN f.addressee_id ELSE f.requester_id END
            WHERE f.requester_id = ? OR f.addressee_id = ?
            ORDER BY LOWER(p.pseudo)
            """,
            rs -> {
                FriendDto dto = new FriendDto(rs.getLong("id"), rs.getString("pseudo"), rs.getString("country"));
                boolean iAmRequester = userId.equals(rs.getString("requester_id"));
                if ("accepted".equals(rs.getString("status"))) friends.add(dto);
                else if (iAmRequester) outgoing.add(dto);
                else incoming.add(dto);
            },
            userId, userId, userId);

        return new Overview(friends, incoming, outgoing);
    }

    /** Envoie une demande d'ami à partir d'un pseudo. */
    @PostMapping("/requests")
    public ResponseEntity<Void> send(@RequestAttribute("userId") String userId,
                                     @RequestBody AddRequest body) {
        String pseudo = normalize(body.pseudo());
        if (pseudo.isEmpty()) return ResponseEntity.badRequest().build();

        List<String> targets = jdbc.query(
            "SELECT user_id FROM profiles WHERE " + NORM_PSEUDO + " = LOWER(?)",
            (rs, i) -> rs.getString("user_id"), pseudo);          // 404

        String targetId = targets.get(0);
        if (targetId.equals(userId)) return ResponseEntity.badRequest().build();  // 400 (soi-même)

        List<Map<String, Object>> existing = jdbc.queryForList("""
            SELECT id, status, requester_id FROM friendships
            WHERE (requester_id = ? AND addressee_id = ?) OR (requester_id = ? AND addressee_id = ?)
            """, userId, targetId, targetId, userId);

        if (!existing.isEmpty()) {
            Map<String, Object> row = existing.get(0);
            boolean theyAskedMe = "pending".equals(row.get("status"))
                                  && targetId.equals(row.get("requester_id"));
            if (theyAskedMe) {  // demandes croisées → on accepte directement
                jdbc.update("UPDATE friendships SET status = 'accepted' WHERE id = ?", row.get("id"));
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.status(409).build();                            // déjà ami / déjà envoyée
        }

        try {
            jdbc.update("INSERT INTO friendships (requester_id, addressee_id) VALUES (?, ?)", userId, targetId);
            return ResponseEntity.ok().build();
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(409).build();                            // course entre deux requêtes
        }
    }

    /** Accepter une demande reçue (seul le destinataire peut le faire). */
    @PostMapping("/{id}/accept")
    public ResponseEntity<Void> accept(@RequestAttribute("userId") String userId, @PathVariable long id) {
        int n = jdbc.update(
            "UPDATE friendships SET status = 'accepted' WHERE id = ? AND addressee_id = ? AND status = 'pending'",
            id, userId);
        return n == 0 ? ResponseEntity.notFound().build() : ResponseEntity.ok().build();
    }

    /** Refuser une demande reçue, annuler une demande envoyée, ou supprimer un ami. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@RequestAttribute("userId") String userId, @PathVariable long id) {
        int n = jdbc.update(
            "DELETE FROM friendships WHERE id = ? AND (requester_id = ? OR addressee_id = ?)",
            id, userId, userId);
        return n == 0 ? ResponseEntity.notFound().build() : ResponseEntity.noContent().build();
    }

    /** Suggestions de pseudos (2 caractères minimum, 8 résultats max). */
    @GetMapping("/search")
    public List<String> search(@RequestAttribute("userId") String userId, @RequestParam String q) {
        String n = normalize(q).toLowerCase();
        if (n.length() < 2) return List.of();
        String like = "%" + n.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";

        return jdbc.query("""
            SELECT p.pseudo FROM profiles p
            WHERE p.user_id <> ?
            AND LOWER(TRANSLATE(p.pseudo, chr(160), ' ')) LIKE ? ESCAPE '\\'
            AND NOT EXISTS (
                SELECT 1 FROM friendships f
                WHERE (f.requester_id = ? AND f.addressee_id = p.user_id)
                OR (f.requester_id = p.user_id AND f.addressee_id = ?))
            ORDER BY LOWER(p.pseudo)
            LIMIT 8
            """,
            (rs, i) -> rs.getString("pseudo"),
            userId, like, userId, userId);
    }
}