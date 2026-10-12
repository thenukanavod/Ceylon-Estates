package com.example.ceylonestate.dto;

import com.example.ceylonestate.model.District;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the "List your property" form can send, for all three property
 * types at once. The form shows/hides the type-specific inputs; the
 * controller checks the ones that matter for the chosen type.
 *
 * Using a separate form object (rather than binding straight onto the
 * entities) keeps users from setting fields they shouldn't, like id,
 * status or owner.
 */
public class ListingForm {

    @NotBlank
    @Pattern(regexp = "land|house|apartment", message = "Choose a property type")
    private String type = "house";

    private boolean forRent;

    @NotBlank
    @Size(max = 120)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotNull
    private District district;

    @NotBlank
    @Size(max = 80)
    private String city;

    @Size(max = 200)
    private String address;

    @NotNull
    @Positive
    private Double price;

    private boolean petFriendly;

    // --- seller contact details shown to buyers ---
    @NotBlank
    @Size(max = 80)
    private String contactName;

    @NotBlank
    @Pattern(regexp = "^[0-9+()\\s-]{7,20}$", message = "Enter a valid phone number")
    private String contactPhone;

    @NotBlank
    @Email
    private String contactEmail;

    // --- houses and apartments ---
    @Min(0)
    @Max(30)
    private Integer bedrooms;

    @Min(0)
    @Max(30)
    private Integer bathrooms;

    @Positive
    private Double floorAreaSqft;

    // --- houses ---
    @Positive
    private Double landSizePerches;
    private boolean hasGarden;

    // --- apartments ---
    @Min(0)
    @Max(120)
    private Integer floorNumber;
    private boolean hasLift;

    // --- land ---
    @Positive
    private Double areaPerches;

    @Size(max = 40)
    private String zoning;

    // --- photos ---
    private List<MultipartFile> images = new ArrayList<>();

    /** Ids of existing photos the seller ticked "remove" on (edit form only). */
    private List<Long> removeImageIds = new ArrayList<>();

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isForRent() { return forRent; }
    public void setForRent(boolean forRent) { this.forRent = forRent; }

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

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public boolean isPetFriendly() { return petFriendly; }
    public void setPetFriendly(boolean petFriendly) { this.petFriendly = petFriendly; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public Integer getBedrooms() { return bedrooms; }
    public void setBedrooms(Integer bedrooms) { this.bedrooms = bedrooms; }

    public Integer getBathrooms() { return bathrooms; }
    public void setBathrooms(Integer bathrooms) { this.bathrooms = bathrooms; }

    public Double getFloorAreaSqft() { return floorAreaSqft; }
    public void setFloorAreaSqft(Double floorAreaSqft) { this.floorAreaSqft = floorAreaSqft; }

    public Double getLandSizePerches() { return landSizePerches; }
    public void setLandSizePerches(Double landSizePerches) { this.landSizePerches = landSizePerches; }

    public boolean isHasGarden() { return hasGarden; }
    public void setHasGarden(boolean hasGarden) { this.hasGarden = hasGarden; }

    public Integer getFloorNumber() { return floorNumber; }
    public void setFloorNumber(Integer floorNumber) { this.floorNumber = floorNumber; }

    public boolean isHasLift() { return hasLift; }
    public void setHasLift(boolean hasLift) { this.hasLift = hasLift; }

    public Double getAreaPerches() { return areaPerches; }
    public void setAreaPerches(Double areaPerches) { this.areaPerches = areaPerches; }

    public String getZoning() { return zoning; }
    public void setZoning(String zoning) { this.zoning = zoning; }

    public List<MultipartFile> getImages() { return images; }
    public void setImages(List<MultipartFile> images) { this.images = images; }

    public List<Long> getRemoveImageIds() { return removeImageIds; }
    public void setRemoveImageIds(List<Long> removeImageIds) { this.removeImageIds = removeImageIds; }
}
