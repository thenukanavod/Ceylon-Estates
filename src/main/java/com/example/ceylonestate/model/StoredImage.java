package com.example.ceylonestate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * One uploaded picture (property photo, broker photo or avatar), stored in the
 * database instead of an "uploads" folder on one person's PC. Because every
 * team member points at the same MySQL database, a photo uploaded by anyone
 * is instantly available to everyone.
 *
 * The id is the same path the browser asks for, e.g. "properties/3f2a...jpg",
 * served back through /uploads/** (see UploadRequestHandler).
 */
@Entity
@Table(name = "stored_images")
public class StoredImage {

    @Id
    @Column(name = "image_key", length = 150, nullable = false)
    private String imageKey;

    @Column(nullable = false, length = 50)
    private String contentType;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] data;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected StoredImage() {
        // required by JPA
    }

    public StoredImage(String imageKey, String contentType, byte[] data) {
        this.imageKey = imageKey;
        this.contentType = contentType;
        this.data = data;
    }

    public String getImageKey() {
        return imageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
