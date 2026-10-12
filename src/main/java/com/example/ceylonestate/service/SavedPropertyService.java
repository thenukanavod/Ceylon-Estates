package com.example.ceylonestate.service;

import com.example.ceylonestate.model.Property;
import com.example.ceylonestate.model.SavedProperty;
import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.PropertyRepository;
import com.example.ceylonestate.repository.SavedPropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Business rules for saved properties: a buyer can save a visible listing once,
 * add a private note, and remove it again.
 */
@Service
public class SavedPropertyService {

    private final SavedPropertyRepository savedRepository;
    private final PropertyRepository propertyRepository;

    public SavedPropertyService(SavedPropertyRepository savedRepository, PropertyRepository propertyRepository) {
        this.savedRepository = savedRepository;
        this.propertyRepository = propertyRepository;
    }

    /** Create or Delete in one click: saves the property if it is not saved yet, removes it if it is. Returns true if it is now saved. */
    @Transactional
    public boolean toggle(User user, Long propertyId) {
        Optional<SavedProperty> existing = savedRepository.findByUserAndPropertyId(user, propertyId);
        if (existing.isPresent()) {
            savedRepository.delete(existing.get());
            return false;
        }
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("saved.error.notFound"));
        if (!property.getStatus().isLive() && !property.getStatus().isCompleted()) {
            throw new IllegalArgumentException("saved.error.notFound");
        }
        savedRepository.save(new SavedProperty(user, property));
        return true;
    }

    /** Read: the buyer's saved items, newest first. */
    @Transactional(readOnly = true)
    public List<SavedProperty> list(User user) {
        return savedRepository.findByUserOrderBySavedAtDesc(user);
    }

    /** Read: ids only, used to fill the hearts on listing cards. */
    @Transactional(readOnly = true)
    public Set<Long> savedIds(User user) {
        return savedRepository.findPropertyIdsByUser(user);
    }

    /** Update: change the private note (an empty note clears it). */
    @Transactional
    public void updateNote(User user, Long savedId, String note) {
        SavedProperty saved = savedRepository.findByIdAndUser(savedId, user)
                .orElseThrow(() -> new IllegalArgumentException("saved.error.notFound"));
        saved.setNote(note);
        savedRepository.save(saved);
    }

    /** Delete: remove one saved item. */
    @Transactional
    public void remove(User user, Long savedId) {
        savedRepository.findByIdAndUser(savedId, user).ifPresent(savedRepository::delete);
    }
}
