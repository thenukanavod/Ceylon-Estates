package com.example.ceylonestate.model;

import com.example.ceylonestate.dto.ListingForm;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.List;

/** A stand-alone house, usually on its own plot of land (measured in perches). */
@Entity
@DiscriminatorValue("HOUSE")
public class HouseProperty extends DwellingProperty {

    private Double landSizePerches;
    private Boolean hasGarden;

    @Override
    public String getTypeKey() {
        return "house";
    }

    @Override
    public String getTagMessageKey() {
        return "home.listings.tagHouse";
    }

    @Override
    public String getExploreMessageKey() {
        return "home.listings.exploreHouse";
    }

    @Override
    public String getIcon() {
        return "bi-house-door-fill";
    }

    @Override
    public String getThumbGradient() {
        return "linear-gradient(135deg, #1565c0, #42a5f5)";
    }

    @Override
    public String getDisplayInfo() {
        return super.getDisplayInfo() + ", " + formatNumber(getLandSizePerches()) + " perches";
    }

    @Override
    public List<PropertyFact> getFacts() {
        List<PropertyFact> facts = super.getFacts();
        facts.add(new PropertyFact("fact.landSize", formatNumber(getLandSizePerches()) + " perches", "bi-rulers"));
        if (isHasGarden()) {
            facts.add(new PropertyFact("fact.garden", "Yes", "bi-flower1"));
        }
        return facts;
    }

    @Override
    protected void applyTypeFields(ListingForm form) {
        super.applyTypeFields(form);
        this.landSizePerches = form.getLandSizePerches();
        this.hasGarden = form.isHasGarden();
    }

    @Override
    protected void fillTypeFields(ListingForm form) {
        super.fillTypeFields(form);
        form.setLandSizePerches(getLandSizePerches());
        form.setHasGarden(isHasGarden());
    }

    public double getLandSizePerches() { return landSizePerches == null ? 0 : landSizePerches; }
    public void setLandSizePerches(double landSizePerches) { this.landSizePerches = landSizePerches; }

    public boolean isHasGarden() { return Boolean.TRUE.equals(hasGarden); }
    public void setHasGarden(boolean hasGarden) { this.hasGarden = hasGarden; }
}
