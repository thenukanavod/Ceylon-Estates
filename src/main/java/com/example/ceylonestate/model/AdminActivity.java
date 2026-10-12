package com.example.ceylonestate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** One line in the admin audit log: who did what, and when. */
@Entity
@Table(name = "admin_activity")
public class AdminActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String adminUsername;

    @Column(nullable = false, length = 200)
    private String action;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected AdminActivity() {
        // required by JPA
    }

    public AdminActivity(String adminUsername, String action) {
        this.adminUsername = adminUsername;
        this.action = action;
    }

    public Long getId() { return id; }
    public String getAdminUsername() { return adminUsername; }
    public String getAction() { return action; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
