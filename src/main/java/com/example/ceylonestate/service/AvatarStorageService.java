package com.example.ceylonestate.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Handles saving and deleting avatar images. They are kept in the shared
 * database (see StoredImageService) so a profile picture uploaded on one PC
 * shows up for everyone using the same database. Served back at
 * /uploads/avatars/<filename> by UploadRequestHandler.
 */
@Service
public class AvatarStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final String FOLDER = "avatars";

    private final StoredImageService images;

    public AvatarStorageService(StoredImageService images) {
        this.images = images;
    }

    /**
     * Saves the uploaded picture under a random, safe filename (never trust the
     * original filename from the browser) and returns that generated filename
     * so it can be stored against the user.
     */
    public String store(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("No file was selected.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG, or WEBP images are allowed.");
        }

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };

        String generatedFilename = UUID.randomUUID() + extension;
        images.save(FOLDER + "/" + generatedFilename, contentType, file.getBytes());
        return generatedFilename;
    }

    /** Deletes a previously stored avatar, e.g. when replacing it or deleting the account. */
    public void delete(String filename) {
        if (filename == null || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return;
        }
        images.delete(FOLDER + "/" + filename);
    }
}
