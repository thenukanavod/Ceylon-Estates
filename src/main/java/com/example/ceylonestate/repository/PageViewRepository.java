package com.example.ceylonestate.repository;

import com.example.ceylonestate.model.PageView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PageViewRepository extends JpaRepository<PageView, Long> {

    List<PageView> findByViewedAtAfter(LocalDateTime since);

    List<PageView> findTop20ByOrderByViewedAtDesc();
}
