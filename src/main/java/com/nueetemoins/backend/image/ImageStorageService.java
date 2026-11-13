package com.nueetemoins.backend.image;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageStorageService {

    private static final Set<String> ALLOWED = Set.of("image/png", "image/jpeg", "image/webp");
    private static final int MAX_SIDE = 900;
    private static final int QUALITY = 90;

    private final S3Client s3;

    @Value("${storage.bucket}") private String bucket;
    @Value("${storage.prefix}") private String prefix;
    
   public String upload(MultipartFile file) throws IOException {
        if (!ALLOWED.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Formats acceptés : png, jpeg, webp");
        }

        byte[] data = compress(file);
        String name = UUID.randomUUID() + ".webp";

        s3.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(prefix + "/" + name)
                .contentType("image/webp")
                .build(),
            RequestBody.fromBytes(data));
        return name;
    }

    private byte[] compress(MultipartFile file) throws IOException {
        ImmutableImage image;
        try (InputStream in = file.getInputStream()) {
            image = ImmutableImage.loader().fromStream(in);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image invalide");
        }
        if (image.width > MAX_SIDE || image.height > MAX_SIDE) {
            image = image.max(MAX_SIDE, MAX_SIDE);
        }
        return image.bytes(WebpWriter.DEFAULT.withQ(QUALITY));
    }
    
    public ResponseInputStream<GetObjectResponse> download(String name) {
        try {
            return s3.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(prefix + "/" + name)
                .build());
        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }
    }
    
    public void deleteByUrl(String url) {
    if (url == null || url.isBlank()) return;
    String marker = "/api/images/";
    int i = url.lastIndexOf(marker);
    if (i < 0) return; 
    String name = url.substring(i + marker.length());
    if (name.isBlank() || name.contains("/") || name.contains("..")) return;
    try {
        s3.deleteObject(DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(prefix + "/" + name)
            .build());
    } catch (Exception e) {
        log.warn("Suppression de l'image {} impossible : {}", name, e.getMessage());
    }
}
}