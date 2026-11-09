package com.example.ceylonestate.shared.repository;

import com.example.ceylonestate.shared.model.StoredImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredImageRepository extends JpaRepository<StoredImage, String> {
}
