package com.example.ceylonestate.member6_it26100001.repository;

import com.example.ceylonestate.member6_it26100001.model.AdminActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminActivityRepository extends JpaRepository<AdminActivity, Long> {

    List<AdminActivity> findTop8ByOrderByCreatedAtDesc();

    List<AdminActivity> findTop100ByOrderByCreatedAtDesc();
}
