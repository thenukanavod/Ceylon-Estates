package com.example.ceylonestate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * A buyer's request to view a property at a chosen date and time.
 *
 * OOP: Encapsulation - private fields; the rules (visit time must be in the
 *      future, status only changes through confirm()/decline()/reschedule())
 *      live inside this class, so no other code can put a request into an
 *      invalid state.
 * The database removes a request automatically when its user or property is deleted.
 */
@Entity
@Table(name = "viewing_request")
public class ViewingRequest {

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

    @Column(nullable = false)
    private LocalDateTime visitAt;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.REQUESTED;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected ViewingRequest() {
        // required by JPA
    }

    public ViewingRequest(User user, Property property, LocalDateTime visitAt, String phone, String note) {
        this.user = user;
        this.property = property;
        setVisitAt(visitAt);
        this.phone = phone == null ? "" : phone.trim();
        this.note = note == null || note.isBlank() ? null : note.trim();
    }

    /** Change the visit time. A past time is refused, and the request goes back to "Requested". */
    public void reschedule(LocalDateTime newVisitAt) {
        setVisitAt(newVisitAt);
        this.status = BookingStatus.REQUESTED;
    }

    public void confirm() {
        this.status = BookingStatus.CONFIRMED;
    }

    public void decline() {
        this.status = BookingStatus.DECLINED;
    }

    private void setVisitAt(LocalDateTime visitAt) {
        if (visitAt == null || !visitAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("booking.error.past");
        }
        this.visitAt = visitAt;
    }

    public boolean isOwnedBy(User other) {
        return other != null && user != null && user.getId() != null && user.getId().equals(other.getId());
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Property getProperty() { return property; }
    public LocalDateTime getVisitAt() { return visitAt; }
    public String getPhone() { return phone; }
    public String getNote() { return note; }
    public BookingStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
