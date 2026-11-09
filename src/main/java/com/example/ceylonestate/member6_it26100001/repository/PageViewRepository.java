package com.example.ceylonestate.member6_it26100001.repository;

import com.example.ceylonestate.member6_it26100001.model.PageView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PageViewRepository extends JpaRepository<PageView, Long> {

    List<PageView> findByViewedAtAfter(LocalDateTime since);

    List<PageView> findTop20ByOrderByViewedAtDesc();
}
