package com.example.ceylonestate.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * File handling for broker profile photos: validates the upload, saves it under
 * uploads/brokers/ with a random name, and deletes it again when it is replaced
 * or the broker is removed. Served back through the /uploads/** handler in WebConfig.
 */
@Service
public class BrokerPhotoStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_BYTES = 2L * 1024 * 1024; // 2 MB
    private static final Path UPLOAD_DIR = Paths.get("uploads", "brokers");
    private static final String URL_PREFIX = "/uploads/brokers/";

    /** Checks and saves the photo; returns the URL to store on the broker. */
    public String store(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG or WEBP photos are allowed.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("The photo must be 2 MB or smaller.");
        }

        Files.createDirectories(UPLOAD_DIR);
        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String filename = UUID.randomUUID() + extension;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, UPLOAD_DIR.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        }
        return URL_PREFIX + filename;
    }

    /** Deletes a photo this service stored. Anything else is ignored. */
    public void delete(String photoUrl) {
        if (photoUrl == null || !photoUrl.startsWith(URL_PREFIX)) {
            return;
        }
        String filename = photoUrl.substring(URL_PREFIX.length());
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return;
        }
        try {
            Files.deleteIfExists(UPLOAD_DIR.resolve(filename));
        } catch (IOException e) {
            // an orphaned file is not worth failing the request over
        }
    }
}
