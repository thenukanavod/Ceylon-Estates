package com.example.ceylonestate.repository;

import com.example.ceylonestate.model.Broker;
import com.example.ceylonestate.model.District;
import com.example.ceylonestate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BrokerRepository extends JpaRepository<Broker, Long> {

    List<Broker> findAllByOrderByNameAsc();

    /** Brokers based in any of the given districts (used for province filtering). */
    List<Broker> findByDistrictInOrderByNameAsc(Collection<District> districts);

    /** The broker profile that a website account signs in as, if any. */
    Optional<Broker> findByAccount(User account);

    /** A broker made from an approved application, found by the application's email, that has no login yet. */
    Optional<Broker> findFirstByEmailIgnoreCaseAndAccountIsNull(String email);
}
