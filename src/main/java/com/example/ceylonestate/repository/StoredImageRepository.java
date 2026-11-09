package com.example.ceylonestate.repository;

import com.example.ceylonestate.model.StoredImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredImageRepository extends JpaRepository<StoredImage, String> {
}
