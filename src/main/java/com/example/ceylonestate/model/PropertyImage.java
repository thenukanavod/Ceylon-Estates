package com.example.ceylonestate.model;

import jakarta.persistence.*;

/**
 * One photo in a property's gallery. The url is either a bundled image
 * (/images/properties/house1.png) or an uploaded file served from the
 * external uploads folder (/uploads/properties/<uuid>.jpg).
 */
@Entity
@Table(name = "property_images")
public class PropertyImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "property_id")
    private Property property;

    protected PropertyImage() {
        // required by JPA
    }

    public PropertyImage(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getId() {
        return id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Property getProperty() {
        return property;
    }

    void setProperty(Property property) {
        this.property = property;
    }
}
