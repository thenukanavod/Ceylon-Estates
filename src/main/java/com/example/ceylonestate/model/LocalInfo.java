package com.example.ceylonestate.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The "Get Local Info" record for one district: how the schools are, what
 * transport is like, whether pet-friendly rentals are common, and so on.
 * There is at most one record per district. Ratings are 1-5 and are
 * illustrative sample data, not official statistics.
 */
@Entity
@Table(name = "local_info")
public class LocalInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private District district;

    @Size(max = 600)
    @Column(length = 600)
    private String summary;

    @Size(max = 600)
    @Column(length = 600)
    private String schoolInfo;

    @Size(max = 600)
    @Column(length = 600)
    private String transportInfo;

    @Size(max = 600)
    @Column(length = 600)
    private String amenitiesInfo;

    @Size(max = 600)
    @Column(length = 600)
    private String safetyInfo;

    @Column(nullable = false)
    private boolean petFriendlyRentalsCommon;

    @Size(max = 600)
    @Column(length = 600)
    private String petFriendlyNotes;

    @Min(1) @Max(5)
    @Column(nullable = false)
    private int schoolRating = 3;

    @Min(1) @Max(5)
    @Column(nullable = false)
    private int transportRating = 3;

    @Min(1) @Max(5)
    @Column(nullable = false)
    private int amenitiesRating = 3;

    @Min(1) @Max(5)
    @Column(nullable = false)
    private int safetyRating = 3;

    public LocalInfo() {
        // needed by JPA and by the admin form
    }

    public Long getId() { return id; }

    public District getDistrict() { return district; }
    public void setDistrict(District district) { this.district = district; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getSchoolInfo() { return schoolInfo; }
    public void setSchoolInfo(String schoolInfo) { this.schoolInfo = schoolInfo; }

    public String getTransportInfo() { return transportInfo; }
    public void setTransportInfo(String transportInfo) { this.transportInfo = transportInfo; }

    public String getAmenitiesInfo() { return amenitiesInfo; }
    public void setAmenitiesInfo(String amenitiesInfo) { this.amenitiesInfo = amenitiesInfo; }

    public String getSafetyInfo() { return safetyInfo; }
    public void setSafetyInfo(String safetyInfo) { this.safetyInfo = safetyInfo; }

    public boolean isPetFriendlyRentalsCommon() { return petFriendlyRentalsCommon; }
    public void setPetFriendlyRentalsCommon(boolean petFriendlyRentalsCommon) { this.petFriendlyRentalsCommon = petFriendlyRentalsCommon; }

    public String getPetFriendlyNotes() { return petFriendlyNotes; }
    public void setPetFriendlyNotes(String petFriendlyNotes) { this.petFriendlyNotes = petFriendlyNotes; }

    public int getSchoolRating() { return schoolRating; }
    public void setSchoolRating(int schoolRating) { this.schoolRating = schoolRating; }

    public int getTransportRating() { return transportRating; }
    public void setTransportRating(int transportRating) { this.transportRating = transportRating; }

    public int getAmenitiesRating() { return amenitiesRating; }
    public void setAmenitiesRating(int amenitiesRating) { this.amenitiesRating = amenitiesRating; }

    public int getSafetyRating() { return safetyRating; }
    public void setSafetyRating(int safetyRating) { this.safetyRating = safetyRating; }
}
