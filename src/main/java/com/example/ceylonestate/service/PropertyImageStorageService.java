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
 * Saves and deletes property photos on disk - the same real file I/O approach
 * as AvatarStorageService, just for the listing galleries. Files go into
 * uploads/properties/ (outside the compiled project) and are served back
 * through the existing /uploads/** resource handler in WebConfig.
 */
@Service
public class PropertyImageStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Path UPLOAD_DIR = Paths.get("uploads", "properties");
    private static final String URL_PREFIX = "/uploads/properties/";

    /**
     * Writes the file to disk under a random name (never trust the original
     * filename) and returns the URL the browser should use to load it.
     */
    public String store(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG or WEBP photos are allowed.");
        }

        Files.createDirectories(UPLOAD_DIR);

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };

        String generatedFilename = UUID.randomUUID() + extension;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, UPLOAD_DIR.resolve(generatedFilename), StandardCopyOption.REPLACE_EXISTING);
        }
        return URL_PREFIX + generatedFilename;
    }

    /**
     * Deletes a stored photo. Only touches files this service created
     * (URLs under /uploads/properties/) - bundled images such as
     * /images/properties/house-1.jpg are never deleted.
     */
    public void delete(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(URL_PREFIX)) {
            return;
        }
        String filename = imageUrl.substring(URL_PREFIX.length());
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return; // never follow a path that tries to leave the folder
        }
        try {
            Files.deleteIfExists(UPLOAD_DIR.resolve(filename));
        } catch (IOException e) {
            // Non-fatal - an orphaned file on disk isn't worth failing the request over
        }
    }
}
