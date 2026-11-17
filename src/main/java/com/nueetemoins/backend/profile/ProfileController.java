package com.nueetemoins.backend.profile;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileController {
    private final JdbcTemplate jdbc;
    public ProfileController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    record ProfileRequest(String pseudo, String country, String language) {}

    @PostMapping("/api/profile")
    public ResponseEntity<?> create(@RequestAttribute("userId") String userId,
                                    @RequestBody ProfileRequest p) {
        try {
            String pseudo = p.pseudo().replaceAll("[\\s\\u00A0]+", " ").trim();
            jdbc.update("INSERT INTO profiles (user_id, pseudo, country, language) VALUES (?,?,?,?)",
                        userId, pseudo, p.country(), p.language());
            return ResponseEntity.ok().build();
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(409).body("Pseudo déjà pris");
        }
    }

    record ProfileResponse(String pseudo, String country, String language) {}

    @GetMapping("/api/profile")
    public ResponseEntity<ProfileResponse> me(@RequestAttribute("userId") String userId) {
        List<ProfileResponse> rows = jdbc.query(
            "SELECT pseudo, country, language FROM profiles WHERE user_id = ?",
            (rs, i) -> new ProfileResponse(rs.getString("pseudo"), rs.getString("country"), rs.getString("language")),
            userId);
        return rows.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(rows.get(0));
    }
}