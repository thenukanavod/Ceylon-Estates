package com.example.ceylonestate.service;

import com.example.ceylonestate.model.District;
import com.example.ceylonestate.model.LocalInfo;
import com.example.ceylonestate.repository.LocalInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * File handling: imports district guides from an uploaded CSV file.
 *
 * Expected columns (first row is the header):
 *   district, summary, schoolInfo, transportInfo, amenitiesInfo, safetyInfo,
 *   schoolRating, transportRating, amenitiesRating, safetyRating
 *
 * Each row is checked on its own. A good row creates a new guide, or updates
 * the existing guide for that district. A bad row is skipped and reported
 * with its line number, so one typo never loses the whole file.
 */
@Service
public class LocalInfoCsvService {

    public static final String HEADER =
            "district,summary,schoolInfo,transportInfo,amenitiesInfo,safetyInfo,schoolRating,transportRating,amenitiesRating,safetyRating";
    private static final int MAX_TEXT = 600;
    private static final int MAX_ROWS = 500;

    /** What happened to an import: how many rows were added or updated, and which lines were skipped. */
    public record ImportResult(int created, int updated, List<String> errors) { }

    private final LocalInfoRepository repository;

    public LocalInfoCsvService(LocalInfoRepository repository) {
        this.repository = repository;
    }

    /** A small example file the admin can download, edit and upload again. */
    public String templateCsv() {
        return HEADER + "\n"
                + "Kandy,\"Hill-country city with cool weather, a famous temple and strong schools.\","
                + "Several top national schools,Frequent buses to Colombo,Large market and hospital,Generally safe,4,4,4,4\n";
    }

    @Transactional
    public ImportResult importCsv(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a CSV file.");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("The file must be a .csv file.");
        }

        int created = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine != null && headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1); // Excel adds an invisible marker to UTF-8 files
            }
            if (headerLine == null || !headerLine.trim().equalsIgnoreCase(HEADER)) {
                throw new IllegalArgumentException("The first row must be exactly: " + HEADER);
            }

            int lineNumber = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                if (lineNumber - 1 > MAX_ROWS) {
                    errors.add("Stopped at line " + lineNumber + ": a file can have at most " + MAX_ROWS + " rows.");
                    break;
                }
                try {
                    List<String> cols = splitCsvLine(line);
                    if (cols.size() != 10) {
                        throw new IllegalArgumentException("expected 10 columns but found " + cols.size());
                    }
                    District district = District.fromText(cols.get(0));
                    if (district == null) {
                        throw new IllegalArgumentException("unknown district '" + cols.get(0) + "'");
                    }
                    LocalInfo info = repository.findByDistrict(district).orElse(null);
                    boolean isNew = info == null;
                    if (isNew) {
                        info = new LocalInfo();
                        info.setDistrict(district);
                    }
                    info.setSummary(text(cols.get(1), "summary"));
                    info.setSchoolInfo(text(cols.get(2), "schoolInfo"));
                    info.setTransportInfo(text(cols.get(3), "transportInfo"));
                    info.setAmenitiesInfo(text(cols.get(4), "amenitiesInfo"));
                    info.setSafetyInfo(text(cols.get(5), "safetyInfo"));
                    info.setSchoolRating(rating(cols.get(6), "schoolRating"));
                    info.setTransportRating(rating(cols.get(7), "transportRating"));
                    info.setAmenitiesRating(rating(cols.get(8), "amenitiesRating"));
                    info.setSafetyRating(rating(cols.get(9), "safetyRating"));
                    repository.save(info);
                    if (isNew) {
                        created++;
                    } else {
                        updated++;
                    }
                } catch (IllegalArgumentException e) {
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                }
            }
        }
        return new ImportResult(created, updated, errors);
    }

    private static String text(String value, String column) {
        String trimmed = value.trim();
        if (trimmed.length() > MAX_TEXT) {
            throw new IllegalArgumentException(column + " is longer than " + MAX_TEXT + " characters");
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int rating(String value, String column) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return 3; // the guide's normal default
        }
        try {
            int rating = Integer.parseInt(trimmed);
            if (rating < 1 || rating > 5) {
                throw new IllegalArgumentException(column + " must be between 1 and 5");
            }
            return rating;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(column + " must be a whole number from 1 to 5");
        }
    }

    /** Splits one CSV line, understanding "quoted, values" and "" as an escaped quote. */
    static List<String> splitCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("a quote was opened but never closed");
        }
        values.add(current.toString());
        return values;
    }
}
