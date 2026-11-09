package com.example.ceylonestate.shared.config;

import com.example.ceylonestate.shared.service.StoredImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * One-time helper: photos uploaded before the switch to database storage sit
 * in the local uploads/ folder (uploads/properties, uploads/brokers,
 * uploads/avatars). On startup this copies any of them that are not in the
 * database yet into the stored_images table, under the same URL as before, so
 * existing listings keep their pictures. Once imported, the folder is no
 * longer used and can be deleted. Safe to run on every start.
 */
@Component
public class LegacyUploadImporter implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyUploadImporter.class);
    private static final Path ROOT = Paths.get("uploads");
    private static final List<String> FOLDERS = List.of("properties", "brokers", "avatars");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final StoredImageService images;

    public LegacyUploadImporter(StoredImageService images) {
        this.images = images;
    }

    @Override
    public void run(String... args) {
        int imported = 0;
        for (String folder : FOLDERS) {
            Path dir = ROOT.resolve(folder);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> files = Files.list(dir)) {
                for (Path file : (Iterable<Path>) files::iterator) {
                    try {
                        if (importFile(folder, file)) {
                            imported++;
                        }
                    } catch (IOException | RuntimeException e) {
                        log.warn("Could not import {}: {}", file, e.getMessage());
                    }
                }
            } catch (IOException | RuntimeException e) {
                log.warn("Could not read {}: {}", dir, e.getMessage());
            }
        }
        if (imported > 0) {
            log.info("Imported {} photo(s) from the local uploads folder into the database.", imported);
        }
    }

    private boolean importFile(String folder, Path file) throws IOException {
        if (!Files.isRegularFile(file) || Files.size(file) > MAX_BYTES) {
            return false;
        }
        String name = file.getFileName().toString();
        String contentType = contentTypeFor(name);
        if (contentType == null) {
            return false;
        }
        String key = folder + "/" + name;
        if (images.exists(key)) {
            return false;
        }
        images.save(key, contentType, Files.readAllBytes(file));
        return true;
    }

    private static String contentTypeFor(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return null;
    }
}
