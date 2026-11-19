package com.nueetemoins.backend.profile;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nueetemoins.backend.profile.dto.PublicProfileResponse;

import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/players") @RequiredArgsConstructor
public class PublicProfileController {
    private final PublicProfileService service;

    @GetMapping("/{pseudo}")
    public PublicProfileResponse get(@PathVariable String pseudo,
                                     @RequestParam(defaultValue = "en") String lang) {
        return service.get(pseudo, lang);
    }
}