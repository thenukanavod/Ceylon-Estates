package com.example.ceylonestate.repository;

import com.example.ceylonestate.model.Broker;
import com.example.ceylonestate.model.District;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BrokerRepository extends JpaRepository<Broker, Long> {

    List<Broker> findAllByOrderByNameAsc();

    /** Brokers based in any of the given districts (used for province filtering). */
    List<Broker> findByDistrictInOrderByNameAsc(Collection<District> districts);
}
