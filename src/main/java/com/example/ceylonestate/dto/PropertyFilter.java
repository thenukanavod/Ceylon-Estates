package com.example.ceylonestate.dto;

/**
 * The search/filter options on the Properties page. The homepage hero search
 * submits to the same page, so the parameter names here are the ones the
 * hero form sends.
 *
 * Everything is a plain String/Double so that a bad value in the URL
 * (?minPriceM=abc is still a 400, but ?district=Narnia is not) never breaks
 * the page - the search service just ignores what it can't understand.
 */
public class PropertyFilter {

    private String q;            // free text: area, project name, district...
    private String type;         // land | house | apartment
    private String district;     // enum name or display name
    private String listingType;  // sale | rent
    private Double minPriceM;    // millions of LKR
    private Double maxPriceM;
    private String beds;         // "1".."5" or "5+"  (meaning "at least")
    private String baths;
    private boolean pet;         // pet-friendly only
    private String sort = "newest"; // newest | priceAsc | priceDesc

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getListingType() { return listingType; }
    public void setListingType(String listingType) { this.listingType = listingType; }

    public Double getMinPriceM() { return minPriceM; }
    public void setMinPriceM(Double minPriceM) { this.minPriceM = minPriceM; }

    public Double getMaxPriceM() { return maxPriceM; }
    public void setMaxPriceM(Double maxPriceM) { this.maxPriceM = maxPriceM; }

    public String getBeds() { return beds; }
    public void setBeds(String beds) { this.beds = beds; }

    public String getBaths() { return baths; }
    public void setBaths(String baths) { this.baths = baths; }

    public boolean isPet() { return pet; }
    public void setPet(boolean pet) { this.pet = pet; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}
