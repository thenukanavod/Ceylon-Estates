package com.example.ceylonestate.repository;

import com.example.ceylonestate.model.AdminActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminActivityRepository extends JpaRepository<AdminActivity, Long> {

    List<AdminActivity> findTop8ByOrderByCreatedAtDesc();

    List<AdminActivity> findTop100ByOrderByCreatedAtDesc();
}
