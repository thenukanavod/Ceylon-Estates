package com.example.ceylonestate.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Handling for broker profile photos: validates the upload, saves it in the
 * shared database under brokers/<random name>, and deletes it again when it is
 * replaced or the broker is removed. Served back through the /uploads/**
 * handler (UploadRequestHandler).
 */
@Service
public class BrokerPhotoStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_BYTES = 2L * 1024 * 1024; // 2 MB
    private static final String FOLDER = "brokers";
    private static final String URL_PREFIX = "/uploads/brokers/";

    private final StoredImageService images;

    public BrokerPhotoStorageService(StoredImageService images) {
        this.images = images;
    }

    /** Checks and saves the photo; returns the URL to store on the broker. */
    public String store(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG or WEBP photos are allowed.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("The photo must be 2 MB or smaller.");
        }

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String filename = UUID.randomUUID() + extension;
        images.save(FOLDER + "/" + filename, contentType, file.getBytes());
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
        images.delete(FOLDER + "/" + filename);
    }
}
