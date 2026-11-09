package com.example.ceylonestate.member3_it26100105.repository;

import com.example.ceylonestate.member3_it26100105.model.BookingStatus;
import com.example.ceylonestate.member3_it26100105.model.ViewingRequest;
import com.example.ceylonestate.member1_it26100453.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ViewingRequestRepository extends JpaRepository<ViewingRequest, Long> {

    /** A buyer's own bookings, soonest visit first. */
    List<ViewingRequest> findByUserOrderByVisitAtAsc(User user);

    Optional<ViewingRequest> findByIdAndUser(Long id, User user);

    /** Admin overview: newest requests first. */
    List<ViewingRequest> findAllByOrderByCreatedAtDesc();

    boolean existsByUserAndPropertyIdAndVisitAt(User user, Long propertyId, LocalDateTime visitAt);

    long countByStatus(BookingStatus status);

    /** True if any row points at this property (so deleting the property would break a foreign key). */
    boolean existsByPropertyId(Long propertyId);
}
