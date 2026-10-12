package com.example.ceylonestate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * One public page view, used for the admin visitor statistics.
 * A random cookie id (no name, no IP address) tells returning visitors apart.
 */
@Entity
@Table(name = "page_view")
public class PageView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String path;

    @Column(nullable = false, length = 40)
    private String visitorId;

    @Column(nullable = false)
    private LocalDateTime viewedAt = LocalDateTime.now();

    /** Mobile, Tablet or Desktop. */
    @Column(length = 20)
    private String device;

    /** Host of the site that sent the visitor, or null for direct visits. */
    @Column(length = 120)
    private String referrer;

    protected PageView() {
        // required by JPA
    }

    public PageView(String path, String visitorId, LocalDateTime viewedAt, String device, String referrer) {
        this.path = path;
        this.visitorId = visitorId;
        this.viewedAt = viewedAt;
        this.device = device;
        this.referrer = referrer;
    }

    public Long getId() { return id; }
    public String getPath() { return path; }
    public String getVisitorId() { return visitorId; }
    public LocalDateTime getViewedAt() { return viewedAt; }
    public String getDevice() { return device; }
    public String getReferrer() { return referrer; }
}
