package com.example.ceylonestate.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Saves and deletes property photos. Photos now live in the shared database
 * (see StoredImageService) rather than in an uploads/ folder on one PC, so
 * everyone running the site against the same database sees the same photos.
 * They are served back through the /uploads/** handler (UploadRequestHandler).
 */
@Service
public class PropertyImageStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final String FOLDER = "properties";
    private static final String URL_PREFIX = "/uploads/properties/";

    private final StoredImageService images;

    public PropertyImageStorageService(StoredImageService images) {
        this.images = images;
    }

    /**
     * Stores the photo under a random name (never trust the original
     * filename) and returns the URL the browser should use to load it.
     */
    public String store(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG or WEBP photos are allowed.");
        }

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };

        String generatedFilename = UUID.randomUUID() + extension;
        images.save(FOLDER + "/" + generatedFilename, contentType, file.getBytes());
        return URL_PREFIX + generatedFilename;
    }

    /**
     * Deletes a stored photo. Only touches photos this service created
     * (URLs under /uploads/properties/) - bundled images such as
     * /images/properties/house-1.jpg are never deleted.
     */
    public void delete(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(URL_PREFIX)) {
            return;
        }
        String filename = imageUrl.substring(URL_PREFIX.length());
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return;
        }
        images.delete(FOLDER + "/" + filename);
    }
}
