package com.example.ceylonestate.service;

import com.example.ceylonestate.model.Broker;
import com.example.ceylonestate.model.BrokerReview;
import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.BrokerRepository;
import com.example.ceylonestate.repository.BrokerReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Create / Read / Update / Delete for broker reviews, plus the average rating
 * numbers the broker directory shows and filters by.
 */
@Service
public class BrokerReviewService {

    /** One broker with its rating summary and reviews, as shown on the directory page. */
    public static class BrokerCard {
        private final Broker broker;
        private final double average;
        private final List<BrokerReview> reviews;
        private final BrokerReview mine;

        BrokerCard(Broker broker, List<BrokerReview> reviews, BrokerReview mine) {
            this.broker = broker;
            this.reviews = reviews;
            this.mine = mine;
            this.average = reviews.isEmpty() ? 0 :
                    reviews.stream().mapToInt(BrokerReview::getRating).average().orElse(0);
        }

        public Broker getBroker() { return broker; }
        public double getAverage() { return average; }
        public long getCount() { return reviews.size(); }
        public int getRounded() { return (int) Math.round(average); }
        public List<BrokerReview> getReviews() { return reviews; }
        /** The signed-in user's own review of this broker, or null. */
        public BrokerReview getMine() { return mine; }
    }

    private final BrokerReviewRepository reviewRepository;
    private final BrokerRepository brokerRepository;

    public BrokerReviewService(BrokerReviewRepository reviewRepository, BrokerRepository brokerRepository) {
        this.reviewRepository = reviewRepository;
        this.brokerRepository = brokerRepository;
    }

    /** Wraps each broker with its average rating and reviews. {@code viewer} may be null. */
    @Transactional(readOnly = true)
    public List<BrokerCard> cards(List<Broker> brokers, User viewer) {
        Map<Long, List<BrokerReview>> byBroker = new HashMap<>();
        for (BrokerReview r : reviewRepository.findAllWithAuthors()) {
            byBroker.computeIfAbsent(r.getBroker().getId(), k -> new ArrayList<>()).add(r);
        }
        List<BrokerCard> cards = new ArrayList<>();
        for (Broker b : brokers) {
            List<BrokerReview> list = byBroker.getOrDefault(b.getId(), List.of());
            BrokerReview mine = null;
            if (viewer != null) {
                mine = list.stream()
                        .filter(r -> r.getAuthor().getId().equals(viewer.getId()))
                        .findFirst().orElse(null);
            }
            cards.add(new BrokerCard(b, list, mine));
        }
        return cards;
    }

    /** Create, or update the user's existing review of this broker. */
    @Transactional
    public void save(User author, Long brokerId, int rating, String comment) {
        Broker broker = brokerRepository.findById(brokerId)
                .orElseThrow(() -> new IllegalArgumentException("That broker no longer exists."));
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Please choose a rating from 1 to 5 stars.");
        }
        BrokerReview existing = reviewRepository.findByBrokerIdAndAuthor(brokerId, author).orElse(null);
        if (existing != null) {
            existing.update(rating, comment);
            reviewRepository.save(existing);
        } else {
            reviewRepository.save(new BrokerReview(broker, author, rating, comment));
        }
    }

    /** Users can only delete their own reviews. */
    @Transactional
    public void deleteOwn(User author, Long reviewId) {
        reviewRepository.findByIdAndAuthor(reviewId, author).ifPresent(reviewRepository::delete);
    }
}
