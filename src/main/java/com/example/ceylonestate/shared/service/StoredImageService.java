package com.example.ceylonestate.shared.service;

import com.example.ceylonestate.shared.model.StoredImage;
import com.example.ceylonestate.shared.repository.StoredImageRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Small wrapper around the stored_images table used by the three photo storage services. */
@Service
public class StoredImageService {

    private final StoredImageRepository repository;

    public StoredImageService(StoredImageRepository repository) {
        this.repository = repository;
    }

    public void save(String key, String contentType, byte[] data) {
        repository.save(new StoredImage(key, contentType, data));
    }

    public Optional<StoredImage> find(String key) {
        return repository.findById(key);
    }

    public boolean exists(String key) {
        return repository.existsById(key);
    }

    public void delete(String key) {
        if (repository.existsById(key)) {
            repository.deleteById(key);
        }
    }
}
