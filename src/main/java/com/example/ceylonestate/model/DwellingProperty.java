package com.example.ceylonestate.model;

import com.example.ceylonestate.dto.ListingForm;
import jakarta.persistence.Entity;

import java.util.List;

/**
 * Anything people live in - the shared middle layer between Property and
 * the two concrete dwelling types (HouseProperty, ApartmentProperty).
 * Bedrooms, bathrooms and floor area are declared once here instead of being
 * copy-pasted into both subclasses: that is a three-level inheritance chain
 * (Property -> DwellingProperty -> HouseProperty).
 *
 * The fields are wrapper types (Integer/Double) on purpose: in a
 * SINGLE_TABLE hierarchy the columns are empty for LandProperty rows, and a
 * primitive int could otherwise be generated as NOT NULL.
 */
@Entity
public abstract class DwellingProperty extends Property {

    private Integer bedrooms;
    private Integer bathrooms;
    private Double floorAreaSqft;

    @Override
    public int getBedroomCount() {
        return getBedrooms();
    }

    @Override
    public int getBathroomCount() {
        return getBathrooms();
    }

    @Override
    public String getDisplayInfo() {
        return getBedrooms() + " bed, " + getBathrooms() + " bath, " + formatNumber(getFloorAreaSqft()) + " sqft";
    }

    @Override
    public List<PropertyFact> getFacts() {
        List<PropertyFact> facts = super.getFacts();
        facts.add(new PropertyFact("fact.bedrooms", String.valueOf(getBedrooms()), "bi-door-closed"));
        facts.add(new PropertyFact("fact.bathrooms", String.valueOf(getBathrooms()), "bi-droplet"));
        facts.add(new PropertyFact("fact.floorArea", formatNumber(getFloorAreaSqft()) + " sqft", "bi-arrows-fullscreen"));
        return facts;
    }

    @Override
    protected void applyTypeFields(ListingForm form) {
        this.bedrooms = form.getBedrooms();
        this.bathrooms = form.getBathrooms();
        this.floorAreaSqft = form.getFloorAreaSqft();
    }

    @Override
    protected void fillTypeFields(ListingForm form) {
        form.setBedrooms(getBedrooms());
        form.setBathrooms(getBathrooms());
        form.setFloorAreaSqft(getFloorAreaSqft());
    }

    public int getBedrooms() { return bedrooms == null ? 0 : bedrooms; }
    public void setBedrooms(int bedrooms) { this.bedrooms = bedrooms; }

    public int getBathrooms() { return bathrooms == null ? 0 : bathrooms; }
    public void setBathrooms(int bathrooms) { this.bathrooms = bathrooms; }

    public double getFloorAreaSqft() { return floorAreaSqft == null ? 0 : floorAreaSqft; }
    public void setFloorAreaSqft(double floorAreaSqft) { this.floorAreaSqft = floorAreaSqft; }
}
