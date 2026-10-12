package com.example.ceylonestate.model;

import com.example.ceylonestate.dto.ListingForm;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.List;

/**
 * A plot of land. In Sri Lanka land is quoted per perch, so for this type
 * the inherited "price" means price PER PERCH, and the total is worked out
 * from the area - the one place where the polymorphic getTotalPrice()
 * behaves differently from the base class.
 */
@Entity
@DiscriminatorValue("LAND")
public class LandProperty extends Property {

    private Double areaPerches;
    private String zoning; // e.g. Residential, Commercial, Agricultural, Mixed

    @Override
    public String getTypeKey() {
        return "land";
    }

    @Override
    public String getTagMessageKey() {
        return "home.listings.tagLand";
    }

    @Override
    public String getExploreMessageKey() {
        return "home.listings.exploreLand";
    }

    @Override
    public String getIcon() {
        return "bi-tree-fill";
    }

    @Override
    public String getThumbGradient() {
        return "linear-gradient(135deg, #2e7d32, #66bb6a)";
    }

    @Override
    public String getDisplayInfo() {
        String zone = (zoning == null || zoning.isBlank()) ? "" : ", " + zoning;
        return formatNumber(getAreaPerches()) + " perches" + zone;
    }

    @Override
    public double getTotalPrice() {
        return getPrice() * getAreaPerches();
    }

    @Override
    public String getPriceNoteKey() {
        return "home.listings.perPerchUpwards";
    }

    @Override
    public List<PropertyFact> getFacts() {
        List<PropertyFact> facts = super.getFacts();
        facts.add(new PropertyFact("fact.area", formatNumber(getAreaPerches()) + " perches", "bi-rulers"));
        if (zoning != null && !zoning.isBlank()) {
            facts.add(new PropertyFact("fact.zoning", zoning, "bi-signpost-split"));
        }
        facts.add(new PropertyFact("fact.pricePerPerch", getPriceFormatted(), "bi-cash"));
        facts.add(new PropertyFact("fact.totalPrice", getTotalPriceFormatted(), "bi-cash-stack"));
        return facts;
    }

    @Override
    protected void applyTypeFields(ListingForm form) {
        this.areaPerches = form.getAreaPerches();
        this.zoning = form.getZoning() == null ? null : form.getZoning().trim();
    }

    @Override
    protected void fillTypeFields(ListingForm form) {
        form.setAreaPerches(getAreaPerches());
        form.setZoning(zoning);
    }

    public double getAreaPerches() { return areaPerches == null ? 0 : areaPerches; }
    public void setAreaPerches(double areaPerches) { this.areaPerches = areaPerches; }

    public String getZoning() { return zoning; }
    public void setZoning(String zoning) { this.zoning = zoning; }
}
