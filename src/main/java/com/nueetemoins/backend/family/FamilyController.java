package com.nueetemoins.backend.family;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nueetemoins.backend.family.dto.FamilyAdminResponse;
import com.nueetemoins.backend.family.dto.FamilyRequest;
import com.nueetemoins.backend.family.dto.FamilyResponse;
import com.nueetemoins.backend.family.dto.FamilyTranslationRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/families")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    @GetMapping
    public List<FamilyResponse> getAll(@RequestParam(required = false) String lang) {
        return familyService.findAll(lang);
    }

    @GetMapping("/admin")
    public List<FamilyAdminResponse> getAllAdmin() { return familyService.findAllAdmin(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FamilyResponse create(@Valid @RequestBody FamilyRequest req,
                                 @RequestParam(required = false) String lang) {
        return familyService.create(req, lang);
    }

    @PutMapping("/{publicId}/translations/{locale}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putTranslation(@PathVariable String publicId, @PathVariable String locale,
                               @Valid @RequestBody FamilyTranslationRequest req) {
        familyService.putTranslation(publicId, locale, req);
    }

    @PutMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable String publicId, @Valid @RequestBody FamilyRequest req) {
        familyService.update(publicId, req);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String publicId) {
        familyService.delete(publicId);
    }
}