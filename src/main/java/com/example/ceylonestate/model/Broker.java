package com.example.ceylonestate.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * A local mortgage broker in the "broker database". Each broker is based in
 * one district, which also tells us their province - that is what the
 * province filter on the homepage and the Brokers page uses.
 */
@Entity
@Table(name = "brokers")
public class Broker {

    /** Avatar colours, the same palette the homepage broker cards use. */
    private static final List<String> AVATAR_GRADIENTS = List.of(
            "linear-gradient(135deg, #5f9ea0, #2e7d32)",
            "linear-gradient(135deg, #1565c0, #42a5f5)",
            "linear-gradient(135deg, #e63946, #ff8fa3)",
            "linear-gradient(135deg, #37474f, #78909c)",
            "linear-gradient(135deg, #2e7d32, #66bb6a)",
            "linear-gradient(135deg, #6a4c93, #b892ff)",
            "linear-gradient(135deg, #b08968, #ddb892)",
            "linear-gradient(135deg, #ff9f1c, #ffbf69)",
            "linear-gradient(135deg, #197278, #57a773)");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String name;

    @NotBlank
    @Email
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String email;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String phone;

    @Size(max = 100)
    @Column(length = 100)
    private String company;

    @Size(max = 40)
    @Column(length = 40)
    private String licenseNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private District district;

    @Size(max = 500)
    @Column(length = 500)
    private String bio;

    /** URL of the uploaded profile photo (under /uploads/brokers/), or null to show the coloured initials instead. */
    @Column(length = 255)
    private String photoUrl;

    public Broker() {
        // needed by JPA and by the admin form
    }

    public Broker(String name, String email, String phone, String company, String licenseNumber,
                  District district, String bio) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.company = company;
        this.licenseNumber = licenseNumber;
        this.district = district;
        this.bio = bio;
    }

    /** Blank object for the "add broker" form. */
    public static Broker blank() {
        return new Broker();
    }

    /** Derived from the district, so a broker's province can never disagree with it. */
    public Province getProvince() {
        return district == null ? null : district.getProvince();
    }

    public String getInitials() {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    public String getAvatarGradient() {
        int index = id == null ? 0 : (int) (id % AVATAR_GRADIENTS.size());
        return AVATAR_GRADIENTS.get(index);
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public District getDistrict() { return district; }
    public void setDistrict(District district) { this.district = district; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
