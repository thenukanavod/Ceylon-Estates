package com.example.ceylonestate.member5_it26100064.repository;

import com.example.ceylonestate.shared.model.District;
import com.example.ceylonestate.member5_it26100064.model.LocalInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocalInfoRepository extends JpaRepository<LocalInfo, Long> {

    Optional<LocalInfo> findByDistrict(District district);

    boolean existsByDistrict(District district);

    List<LocalInfo> findAllByOrderByDistrictAsc();
}
