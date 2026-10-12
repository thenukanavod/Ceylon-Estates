package com.example.ceylonestate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * A property a signed-in buyer has saved ("favourited") to look at later,
 * with an optional private note. One row per (user, property) pair.
 *
 * OOP: Encapsulation - private fields, the note is trimmed and length-limited
 *      inside its setter so no caller can store an oversized note.
 * The database removes a saved row automatically when its user or its
 * property is deleted (ON DELETE CASCADE), so deleting accounts or listings
 * never fails because of saved items.
 */
@Entity
@Table(name = "saved_property",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "property_id"}))
public class SavedProperty {

    public static final int MAX_NOTE_LENGTH = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Property property;

    @Column(length = MAX_NOTE_LENGTH)
    private String note;

    @Column(nullable = false)
    private LocalDateTime savedAt = LocalDateTime.now();

    protected SavedProperty() {
        // required by JPA
    }

    public SavedProperty(User user, Property property) {
        this.user = user;
        this.property = property;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Property getProperty() { return property; }
    public String getNote() { return note; }
    public LocalDateTime getSavedAt() { return savedAt; }

    public void setNote(String note) {
        if (note == null || note.isBlank()) {
            this.note = null;
            return;
        }
        String trimmed = note.trim();
        this.note = trimmed.length() > MAX_NOTE_LENGTH ? trimmed.substring(0, MAX_NOTE_LENGTH) : trimmed;
    }
}
