package com.example.ceylonestate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
 * A star rating (1-5) and optional comment a signed-in user leaves on a broker.
 * One review per (user, broker): posting again updates the existing one.
 *
 * OOP: Encapsulation - the rating is clamped to 1-5 and the comment trimmed and
 * length-limited inside the setters, so no caller can store invalid data.
 * ON DELETE CASCADE removes reviews when their broker or author is deleted.
 */
@Entity
@Table(name = "broker_review",
        uniqueConstraints = @UniqueConstraint(columnNames = {"broker_id", "author_id"}))
public class BrokerReview {

    public static final int MAX_COMMENT_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Broker broker;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User author;

    @Column(nullable = false)
    private int rating;

    @Column(length = MAX_COMMENT_LENGTH)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected BrokerReview() {
        // required by JPA
    }

    public BrokerReview(Broker broker, User author, int rating, String comment) {
        this.broker = broker;
        this.author = author;
        setRating(rating);
        setComment(comment);
    }

    public Long getId() { return id; }
    public Broker getBroker() { return broker; }
    public User getAuthor() { return author; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setRating(int rating) {
        this.rating = Math.max(1, Math.min(5, rating));
    }

    public void setComment(String comment) {
        if (comment == null || comment.isBlank()) {
            this.comment = null;
            return;
        }
        String trimmed = comment.trim();
        this.comment = trimmed.length() > MAX_COMMENT_LENGTH ? trimmed.substring(0, MAX_COMMENT_LENGTH) : trimmed;
    }

    /** Called when a user edits their existing review. */
    public void update(int rating, String comment) {
        setRating(rating);
        setComment(comment);
        this.createdAt = LocalDateTime.now();
    }
}
