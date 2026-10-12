package com.example.ceylonestate.service;

import com.example.ceylonestate.model.BookingStatus;
import com.example.ceylonestate.model.Property;
import com.example.ceylonestate.model.User;
import com.example.ceylonestate.model.ViewingRequest;
import com.example.ceylonestate.repository.PropertyRepository;
import com.example.ceylonestate.repository.ViewingRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Business rules for viewing bookings. Errors are thrown as
 * IllegalArgumentException carrying a message key, which the controller shows
 * to the user in their own language.
 */
@Service
public class ViewingService {

    private static final int MAX_DAYS_AHEAD = 90;

    private final ViewingRequestRepository bookingRepository;
    private final PropertyRepository propertyRepository;

    public ViewingService(ViewingRequestRepository bookingRepository, PropertyRepository propertyRepository) {
        this.bookingRepository = bookingRepository;
        this.propertyRepository = propertyRepository;
    }

    /** Create: book a viewing for a live listing. */
    @Transactional
    public ViewingRequest book(User user, Long propertyId, LocalDateTime visitAt, String phone, String note) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("booking.error.notFound"));
        if (!property.getStatus().isLive()) {
            throw new IllegalArgumentException("booking.error.notAvailable");
        }
        if (property.isOwnedBy(user)) {
            throw new IllegalArgumentException("booking.error.own");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("booking.error.phone");
        }
        checkWindow(visitAt);
        if (bookingRepository.existsByUserAndPropertyIdAndVisitAt(user, propertyId, visitAt)) {
            throw new IllegalArgumentException("booking.error.duplicate");
        }
        return bookingRepository.save(new ViewingRequest(user, property, visitAt, phone, note));
    }

    /** Read: a buyer's own bookings. */
    @Transactional(readOnly = true)
    public List<ViewingRequest> forUser(User user) {
        return bookingRepository.findByUserOrderByVisitAtAsc(user);
    }

    /** Update: the buyer picks a new time; the request goes back to "Requested". */
    @Transactional
    public void reschedule(User user, Long id, LocalDateTime newVisitAt) {
        ViewingRequest booking = ownBooking(user, id);
        checkWindow(newVisitAt);
        booking.reschedule(newVisitAt);
        bookingRepository.save(booking);
    }

    /** Delete: the buyer cancels (removes) their booking. */
    @Transactional
    public void cancel(User user, Long id) {
        bookingRepository.delete(ownBooking(user, id));
    }

    // ---------- Admin side ----------

    @Transactional(readOnly = true)
    public List<ViewingRequest> all() {
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long countRequested() {
        return bookingRepository.countByStatus(BookingStatus.REQUESTED);
    }

    @Transactional
    public void setStatus(Long id, BookingStatus status) {
        ViewingRequest booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("booking.error.notFound"));
        if (status == BookingStatus.CONFIRMED) {
            booking.confirm();
        } else if (status == BookingStatus.DECLINED) {
            booking.decline();
        }
        bookingRepository.save(booking);
    }

    @Transactional
    public void adminDelete(Long id) {
        bookingRepository.deleteById(id);
    }

    // ---------- helpers ----------

    private ViewingRequest ownBooking(User user, Long id) {
        return bookingRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("booking.error.notFound"));
    }

    private void checkWindow(LocalDateTime visitAt) {
        if (visitAt == null || !visitAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("booking.error.past");
        }
        if (visitAt.isAfter(LocalDateTime.now().plusDays(MAX_DAYS_AHEAD))) {
            throw new IllegalArgumentException("booking.error.tooFar");
        }
    }
}
