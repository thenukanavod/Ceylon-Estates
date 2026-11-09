package com.example.ceylonestate.member4_it26100746.repository;

import com.example.ceylonestate.member4_it26100746.model.BrokerReview;
import com.example.ceylonestate.member1_it26100453.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BrokerReviewRepository extends JpaRepository<BrokerReview, Long> {

    /** All reviews with their authors, newest first (grouped by broker in the service). */
    @Query("select r from BrokerReview r join fetch r.author join fetch r.broker order by r.createdAt desc")
    List<BrokerReview> findAllWithAuthors();

    Optional<BrokerReview> findByBrokerIdAndAuthor(Long brokerId, User author);

    Optional<BrokerReview> findByIdAndAuthor(Long id, User author);
}
