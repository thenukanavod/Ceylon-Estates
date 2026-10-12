package com.example.ceylonestate.repository;

import com.example.ceylonestate.model.ListingStatus;
import com.example.ceylonestate.model.Property;
import com.example.ceylonestate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Works across the whole Property hierarchy (houses, apartments and land)
 * through the one shared table - a query for "Property" returns the right
 * subclass objects automatically.
 */
public interface PropertyRepository extends JpaRepository<Property, Long> {

    /** Newest first. Used for the live listings and the Completed Projects library. */
    List<Property> findByStatusInOrderByListingDateDescIdDesc(Collection<ListingStatus> statuses);

    /** Admin review queue: oldest submission first. */
    List<Property> findByStatusOrderByIdAsc(ListingStatus status);

    /** A seller's own listings, newest first. */
    List<Property> findByOwnerOrderByIdDesc(User owner);

    /** Every listing, newest first (admin overview). */
    List<Property> findAllByOrderByIdDesc();

    long countByStatus(ListingStatus status);

    /** Adds one to a listing's view counter in a single database step (safe when many people open it at once). */
    @Modifying
    @Transactional
    @Query("update Property p set p.viewCount = p.viewCount + 1 where p.id = ?1")
    void incrementViewCount(Long id);
}
