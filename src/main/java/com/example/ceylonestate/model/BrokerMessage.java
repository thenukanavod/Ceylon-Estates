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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * One message in a broker's inbox. It is sent by a signed-in user and may also
 * carry a shared listing. Only the broker the message was sent to can read it.
 */
@Entity
@Table(name = "broker_messages")
public class BrokerMessage {

    public static final int MAX_BODY_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The inbox this message lands in. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Broker broker;

    /** Who sent it (always the signed-in account, never typed in by the sender). */
    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User sender;

    /** A listing shared with the broker. Optional; cleared if the listing is later deleted. */
    @ManyToOne
    @JoinColumn(name = "property_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Property property;

    /** The note. May be empty when the sender only shared a listing. */
    @Column(length = MAX_BODY_LENGTH)
    private String body;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Null until the broker opens or marks it as read. */
    @Column
    private LocalDateTime readAt;

    protected BrokerMessage() {
        // required by JPA
    }

    public BrokerMessage(Broker broker, User sender, Property property, String body) {
        this.broker = broker;
        this.sender = sender;
        this.property = property;
        this.body = body;
    }

    public boolean isUnread() {
        return readAt == null;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }

    public void markUnread() {
        readAt = null;
    }

    public Long getId() { return id; }
    public Broker getBroker() { return broker; }
    public User getSender() { return sender; }
    public Property getProperty() { return property; }
    public String getBody() { return body; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReadAt() { return readAt; }
}
