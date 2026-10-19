package com.example.ceylonestate.model;

import com.example.ceylonestate.dto.ListingForm;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Abstract base class for everything that can be listed on Ceylon Estates.
 *
 * OOP: Encapsulation - every field is private; the price setter refuses
 *      negative values, and photos can only be added through addImage().
 * OOP: Inheritance - HouseProperty, ApartmentProperty and LandProperty extend
 *      this (houses and apartments via the intermediate DwellingProperty),
 *      so the shared fields and behaviour live in exactly one place.
 * OOP: Polymorphism - the abstract methods below (getTypeKey(), getDisplayInfo(),
 *      getFacts(), ...) are implemented differently by each subclass. Pages
 *      just call them on a Property and get the right answer for whatever the
 *      listing really is - no "if type == house" anywhere in the templates.
 *
 * Stored in one database table (SINGLE_TABLE inheritance) with a
 * "property_type" column recording which subclass each row is - the same
 * approach the User hierarchy uses.
 */
@Entity
@Table(name = "properties")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "property_type")
public abstract class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private District district;

    @Column(length = 80)
    private String city;

    @Column(length = 200)
    private String address;

    /** LKR. For houses/apartments the whole price (or monthly rent); for land, the price PER PERCH. */
    @Column(nullable = false)
    private double price;

    /** What the seller wants: false = sell, true = rent out. Kept separately so it survives PENDING. */
    @Column(nullable = false)
    private boolean forRent;

    @Column(nullable = false)
    private boolean petFriendly;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status = ListingStatus.PENDING;

    private LocalDate listingDate = LocalDate.now();

    @Column(length = 80)
    private String contactName;

    @Column(length = 30)
    private String contactPhone;

    @Column(length = 120)
    private String contactEmail;

    /** Set by an admin when a listing is rejected, so the seller knows what to fix. */
    @Column(length = 500)
    private String rejectionReason;

    /** The account that submitted the listing. Null for the sample listings created by the seeder. */
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    /** How many times buyers have opened this listing's page (the seller and admins are not counted). */
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int viewCount;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PropertyImage> images = new ArrayList<>();

    // ------------------------------------------------------------------
    // Polymorphic behaviour - every subclass answers these for itself
    // ------------------------------------------------------------------

    /** "land", "house" or "apartment" - used for filtering and CSS hooks. */
    public abstract String getTypeKey();

    /** messages.properties key for the small tag on cards (LAND / HOUSE / APARTMENT). */
    public abstract String getTagMessageKey();

    /** messages.properties key for the "Explore ..." link text. */
    public abstract String getExploreMessageKey();

    /** Bootstrap Icons class for this type. */
    public abstract String getIcon();

    /** Fallback card background when a listing has no photo (same colours as the homepage). */
    public abstract String getThumbGradient();

    /** One-line summary of the type-specific details, e.g. "3 bed, 2 bath, 2,200 sqft". */
    public abstract String getDisplayInfo();

    /** Type-specific copy of the submitted form into this object. */
    protected abstract void applyTypeFields(ListingForm form);

    /** Type-specific copy of this object back into a form (used to pre-fill the edit page). */
    protected abstract void fillTypeFields(ListingForm form);

    /** Type-specific rows for the key-facts grid on the detail page. */
    public List<PropertyFact> getFacts() {
        return new ArrayList<>();
    }

    /** Bedrooms - 0 unless the subclass is something people sleep in. Lets filters ignore the type. */
    public int getBedroomCount() {
        return 0;
    }

    public int getBathroomCount() {
        return 0;
    }

    /** Land overrides this: its stored price is per perch, so the total is price x area. */
    public double getTotalPrice() {
        return price;
    }

    /** messages.properties key for the small line under the price. Land overrides it. */
    public String getPriceNoteKey() {
        return forRent ? "listing.perMonth" : "home.listings.fixedPrice";
    }

    // ------------------------------------------------------------------
    // Template method: shared copying + type-specific copying
    // ------------------------------------------------------------------

    /** Copies a submitted form onto this listing (create and edit both use this). */
    public void updateFrom(ListingForm form) {
        this.title = form.getTitle().trim();
        this.description = form.getDescription() == null ? null : form.getDescription().trim();
        this.district = form.getDistrict();
        this.city = form.getCity() == null ? null : form.getCity().trim();
        this.address = form.getAddress() == null ? null : form.getAddress().trim();
        setPrice(form.getPrice());
        this.forRent = form.isForRent();
        this.petFriendly = form.isPetFriendly();
        this.contactName = form.getContactName().trim();
        this.contactPhone = form.getContactPhone().trim();
        this.contactEmail = form.getContactEmail().trim();
        applyTypeFields(form);
    }

    /** Builds a pre-filled form from this listing. */
    public ListingForm toForm() {
        ListingForm form = new ListingForm();
        form.setType(getTypeKey());
        form.setForRent(forRent);
        form.setTitle(title);
        form.setDescription(description);
        form.setDistrict(district);
        form.setCity(city);
        form.setAddress(address);
        form.setPrice(price);
        form.setPetFriendly(petFriendly);
        form.setContactName(contactName);
        form.setContactPhone(contactPhone);
        form.setContactEmail(contactEmail);
        fillTypeFields(form);
        return form;
    }

    // ------------------------------------------------------------------
    // Convenience for the templates
    // ------------------------------------------------------------------

    public void addImage(PropertyImage image) {
        image.setProperty(this);
        this.images.add(image);
    }

    public String getCoverImageUrl() {
        return images.isEmpty() ? null : images.get(0).getImageUrl();
    }

    public String getLocationLabel() {
        String districtName = district == null ? "" : district.getLabel();
        if (city == null || city.isBlank() || city.equalsIgnoreCase(districtName)) {
            return districtName;
        }
        return city + ", " + districtName;
    }

    /** "8,200,000 LKR" - the format the homepage cards use. */
    public String getPriceFormatted() {
        return formatNumber(price) + " LKR";
    }

    public String getTotalPriceFormatted() {
        return formatNumber(getTotalPrice()) + " LKR";
    }

    public boolean isOwnedBy(User user) {
        return owner != null && user != null && owner.getId() != null && owner.getId().equals(user.getId());
    }

    /** "1,234" or "1,234.5" - no pointless ".0". */
    protected static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.format(Locale.ENGLISH, "%,.0f", value);
        }
        return String.format(Locale.ENGLISH, "%,.1f", value);
    }

    // ------------------------------------------------------------------
    // Getters and setters
    // ------------------------------------------------------------------

    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public District getDistrict() { return district; }
    public void setDistrict(District district) { this.district = district; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getPrice() { return price; }

    /** Encapsulation: the object guards its own data - a negative price is never allowed in. */
    public void setPrice(double price) {
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        this.price = price;
    }

    public boolean isForRent() { return forRent; }
    public void setForRent(boolean forRent) { this.forRent = forRent; }

    public boolean isPetFriendly() { return petFriendly; }
    public void setPetFriendly(boolean petFriendly) { this.petFriendly = petFriendly; }

    public ListingStatus getStatus() { return status; }
    public void setStatus(ListingStatus status) { this.status = status; }

    public LocalDate getListingDate() { return listingDate; }
    public void setListingDate(LocalDate listingDate) { this.listingDate = listingDate; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public List<PropertyImage> getImages() { return images; }
}
