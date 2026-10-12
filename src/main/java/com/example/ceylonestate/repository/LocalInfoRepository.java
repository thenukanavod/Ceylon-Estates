package com.example.ceylonestate.repository;

import com.example.ceylonestate.model.District;
import com.example.ceylonestate.model.LocalInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocalInfoRepository extends JpaRepository<LocalInfo, Long> {

    Optional<LocalInfo> findByDistrict(District district);

    boolean existsByDistrict(District district);

    List<LocalInfo> findAllByOrderByDistrictAsc();
}
