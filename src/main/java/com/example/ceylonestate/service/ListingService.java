package com.example.ceylonestate.service;

import com.example.ceylonestate.dto.ListingForm;
import com.example.ceylonestate.model.ApartmentProperty;
import com.example.ceylonestate.model.HouseProperty;
import com.example.ceylonestate.model.LandProperty;
import com.example.ceylonestate.model.ListingStatus;
import com.example.ceylonestate.model.Property;
import com.example.ceylonestate.model.PropertyImage;
import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The create / update / review / delete rules for listings, kept out of the
 * controllers so every screen (seller pages and admin pages) follows the same
 * rules.
 *
 * Life of a listing:  seller submits -> PENDING -> admin approves ->
 * FOR_SALE / FOR_RENT -> seller marks it done -> SOLD / RENTED.
 * An admin can instead reject it (REJECTED, with a reason); when the seller
 * edits a rejected listing it goes back to PENDING for another look.
 */
@Service
public class ListingService {

    public static final int MAX_PHOTOS = 8;

    private final PropertyRepository propertyRepository;
    private final PropertyImageStorageService imageStorage;

    public ListingService(PropertyRepository propertyRepository, PropertyImageStorageService imageStorage) {
        this.propertyRepository = propertyRepository;
        this.imageStorage = imageStorage;
    }

    /**
     * Factory: picks the right subclass for the type chosen on the form.
     * The caller just gets "a Property" and never needs to know which kind.
     */
    private Property newInstanceFor(String type) {
        return switch (type) {
            case "land" -> new LandProperty();
            case "apartment" -> new ApartmentProperty();
            default -> new HouseProperty();
        };
    }

    @Transactional(rollbackFor = IOException.class)
    public Property create(ListingForm form, User owner) throws IOException {
        Property property = newInstanceFor(form.getType());
        property.updateFrom(form);
        property.setOwner(owner);
        property.setStatus(ListingStatus.PENDING);
        property.setListingDate(LocalDate.now());
        addPhotos(property, form.getImages());
        return propertyRepository.save(property);
    }

    @Transactional(rollbackFor = IOException.class)
    public Property update(Property property, ListingForm form) throws IOException {
        property.updateFrom(form);

        // A rejected listing that has been fixed goes back into the review queue.
        if (property.getStatus() == ListingStatus.REJECTED) {
            property.setStatus(ListingStatus.PENDING);
            property.setRejectionReason(null);
        }

        // Remove the photos the seller ticked, and delete their files.
        List<Long> toRemove = form.getRemoveImageIds() == null ? List.of() : form.getRemoveImageIds();
        List<PropertyImage> removed = new ArrayList<>();
        for (PropertyImage image : property.getImages()) {
            if (toRemove.contains(image.getId())) {
                removed.add(image);
            }
        }
        property.getImages().removeAll(removed); // orphanRemoval deletes the rows
        removed.forEach(image -> imageStorage.delete(image.getImageUrl()));

        addPhotos(property, form.getImages());
        return propertyRepository.save(property);
    }

    /** Stores the newly uploaded photos, up to the per-listing limit, ignoring empty file inputs. */
    private void addPhotos(Property property, List<MultipartFile> files) throws IOException {
        if (files == null) {
            return;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (property.getImages().size() >= MAX_PHOTOS) {
                break;
            }
            property.addImage(new PropertyImage(imageStorage.store(file)));
        }
    }

    /** Admin: lets a pending listing go live as "for sale" or "for rent", whichever the seller asked for. */
    @Transactional
    public void approve(Property property) {
        property.setStatus(property.isForRent() ? ListingStatus.FOR_RENT : ListingStatus.FOR_SALE);
        property.setRejectionReason(null);
        property.setListingDate(LocalDate.now());
        propertyRepository.save(property);
    }

    /** Admin: sends a listing back with a reason the seller can read. */
    @Transactional
    public void reject(Property property, String reason) {
        property.setStatus(ListingStatus.REJECTED);
        String trimmed = reason == null ? "" : reason.trim();
        property.setRejectionReason(trimmed.isEmpty() ? "Please review the details and resubmit." : trimmed);
        propertyRepository.save(property);
    }

    /** Seller: the property has sold (or been rented out) - it moves into the Completed Projects library. */
    @Transactional
    public void markCompleted(Property property) {
        if (property.getStatus().isLive()) {
            property.setStatus(property.isForRent() ? ListingStatus.RENTED : ListingStatus.SOLD);
            propertyRepository.save(property);
        }
    }

    /** Seller: copy a listing's details into a new listing that goes to review as PENDING (photos are not copied). */
    @Transactional
    public Property duplicate(Property original) {
        Property copy = newInstanceFor(original.getTypeKey());
        copy.updateFrom(original.toForm());
        String title = "Copy of " + original.getTitle();
        copy.setTitle(title.length() > 120 ? title.substring(0, 120) : title);
        copy.setOwner(original.getOwner());
        copy.setStatus(ListingStatus.PENDING);
        copy.setListingDate(LocalDate.now());
        return propertyRepository.save(copy);
    }

    @Transactional
    public void delete(Property property) {
        List<String> urls = property.getImages().stream().map(PropertyImage::getImageUrl).toList();
        propertyRepository.delete(property);
        urls.forEach(imageStorage::delete);
    }
}
