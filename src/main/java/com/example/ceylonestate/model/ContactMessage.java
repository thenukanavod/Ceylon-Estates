package com.example.ceylonestate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * One message sent through the public "Contact us" form. Admins read, answer,
 * archive and delete these from the admin inbox.
 */
@Entity
@Table(name = "contact_message")
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, length = 150)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status = MessageStatus.NEW;

    /** The answer an admin emailed back (null until replied). */
    @Column(length = 2000)
    private String adminReply;

    private LocalDateTime repliedAt;

    @Column(length = 80)
    private String repliedBy;

    protected ContactMessage() {
        // required by JPA
    }

    public ContactMessage(String name, String email, String phone, String subject, String message) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.subject = subject;
        this.message = message;
    }

    /** Records that an admin answered this message. */
    public void markReplied(String reply, String adminUsername) {
        this.adminReply = reply;
        this.repliedBy = adminUsername;
        this.repliedAt = LocalDateTime.now();
        if (this.status == MessageStatus.NEW) {
            this.status = MessageStatus.READ;
        }
    }

    public boolean isReplied() { return repliedAt != null; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }
    public String getAdminReply() { return adminReply; }
    public LocalDateTime getRepliedAt() { return repliedAt; }
    public String getRepliedBy() { return repliedBy; }
}
