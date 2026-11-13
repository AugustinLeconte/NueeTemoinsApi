package com.nueetemoins.backend.image;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageStorageService storage;

    @Value("${storage.public-base-url}")
    private String publicBaseUrl;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String name = storage.upload(file);
        return Map.of("url", publicBaseUrl + "/api/images/" + name);
    }

    @GetMapping("/{name:.+}")
    public ResponseEntity<InputStreamResource> get(@PathVariable String name) {
        if (name.contains("/") || name.contains("..")) {
            return ResponseEntity.badRequest().build();
        }
        var object = storage.download(name);
        var meta = object.response();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(meta.contentType()))
            .contentLength(meta.contentLength())
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
            .body(new InputStreamResource(object));
    }
}