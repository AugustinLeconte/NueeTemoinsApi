package com.nueetemoins.backend.profile;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nueetemoins.backend.profile.ProfileService.Info;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ProfileController {
    private final JdbcTemplate jdbc;
    private final ProfileService profileService;

    record ProfileRequest(String pseudo, String country, String language) {}
    record PatronRequest(String cardId) {}

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

    @GetMapping("/api/profile")
    public Info me(@RequestAttribute("userId") String userId) {
        return profileService.require(userId);
    }

    @PutMapping("/api/profile/patron")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPatron(@RequestAttribute("userId") String userId, @RequestBody PatronRequest r) {
        profileService.setPatron(userId, r.cardId());
    }
}