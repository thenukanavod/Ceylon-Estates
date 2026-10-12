package com.example.ceylonestate.model;

import com.example.ceylonestate.dto.ListingForm;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.List;

/** A flat inside a larger building - knows which floor it is on and whether there is a lift. */
@Entity
@DiscriminatorValue("APARTMENT")
public class ApartmentProperty extends DwellingProperty {

    private Integer floorNumber;
    private Boolean hasLift;

    @Override
    public String getTypeKey() {
        return "apartment";
    }

    @Override
    public String getTagMessageKey() {
        return "home.listings.tagApartment";
    }

    @Override
    public String getExploreMessageKey() {
        return "home.listings.exploreApartment";
    }

    @Override
    public String getIcon() {
        return "bi-building-fill";
    }

    @Override
    public String getThumbGradient() {
        return "linear-gradient(135deg, #37474f, #78909c)";
    }

    @Override
    public String getDisplayInfo() {
        return super.getDisplayInfo() + ", floor " + getFloorNumber() + (isHasLift() ? " (lift)" : "");
    }

    @Override
    public List<PropertyFact> getFacts() {
        List<PropertyFact> facts = super.getFacts();
        facts.add(new PropertyFact("fact.floor", String.valueOf(getFloorNumber()), "bi-layers"));
        if (isHasLift()) {
            facts.add(new PropertyFact("fact.lift", "Yes", "bi-arrow-down-up"));
        }
        return facts;
    }

    @Override
    protected void applyTypeFields(ListingForm form) {
        super.applyTypeFields(form);
        this.floorNumber = form.getFloorNumber();
        this.hasLift = form.isHasLift();
    }

    @Override
    protected void fillTypeFields(ListingForm form) {
        super.fillTypeFields(form);
        form.setFloorNumber(getFloorNumber());
        form.setHasLift(isHasLift());
    }

    public int getFloorNumber() { return floorNumber == null ? 0 : floorNumber; }
    public void setFloorNumber(int floorNumber) { this.floorNumber = floorNumber; }

    public boolean isHasLift() { return Boolean.TRUE.equals(hasLift); }
    public void setHasLift(boolean hasLift) { this.hasLift = hasLift; }
}
