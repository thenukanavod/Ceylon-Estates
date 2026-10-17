package com.example.ceylonestate.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * A signed-in user's request to be listed as a broker. An admin approves it
 * (which creates a public {@link Broker}) or rejects it.
 *
 * OOP: Encapsulation - the status only changes through approve()/reject(), and
 * the text setters trim their input, so the object cannot be left half-valid.
 */
@Entity
@Table(name = "broker_applications")
public class BrokerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User applicant;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(length = 100)
    private String company;

    @Column(length = 40)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private District district;

    @Column(length = 500)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected BrokerApplication() {
        // required by JPA
    }

    public BrokerApplication(User applicant, String name, String email, String phone, String company,
                             String licenseNumber, District district, String bio) {
        this.applicant = applicant;
        setName(name);
        setEmail(email);
        setPhone(phone);
        setCompany(company);
        setLicenseNumber(licenseNumber);
        this.district = district;
        setBio(bio);
    }

    /** Builds the public broker profile from this application. */
    public Broker toBroker() {
        return new Broker(name, email, phone, company, licenseNumber, district, bio);
    }

    public void approve() {
        this.status = ApplicationStatus.APPROVED;
    }

    public void reject() {
        this.status = ApplicationStatus.REJECTED;
    }

    private static String clean(String value, int max) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > max ? t.substring(0, max) : t;
    }

    public Long getId() { return id; }
    public User getApplicant() { return applicant; }
    public String getName() { return name; }
    public void setName(String name) { this.name = clean(name, 80); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = clean(email, 120); }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = clean(phone, 30); }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = clean(company, 100); }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = clean(licenseNumber, 40); }
    public District getDistrict() { return district; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = clean(bio, 500); }
    public ApplicationStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
