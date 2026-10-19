package com.example.ceylonestate.repository;

import com.example.ceylonestate.model.SavedProperty;
import com.example.ceylonestate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SavedPropertyRepository extends JpaRepository<SavedProperty, Long> {

    /** A buyer's saved items, most recently saved first. */
    List<SavedProperty> findByUserOrderBySavedAtDesc(User user);

    Optional<SavedProperty> findByUserAndPropertyId(User user, Long propertyId);

    Optional<SavedProperty> findByIdAndUser(Long id, User user);

    /** True if any row points at this property (so deleting the property would break a foreign key). */
    boolean existsByPropertyId(Long propertyId);

    /** Just the property ids, so cards can show a filled heart without loading everything. */
    @Query("select s.property.id from SavedProperty s where s.user = ?1")
    Set<Long> findPropertyIdsByUser(User user);
}
