package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.Broker;
import com.example.ceylonestate.model.District;
import com.example.ceylonestate.model.Province;
import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.BrokerRepository;
import com.example.ceylonestate.service.BrokerReviewService;
import com.example.ceylonestate.service.BrokerReviewService.BrokerCard;
import com.example.ceylonestate.service.CurrentUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The public mortgage broker directory: browse, search, filter by province and
 * by star rating, read other people's comments, and (signed in) rate a broker.
 *   GET  /brokers                       - Read (directory + reviews)
 *   POST /brokers/{id}/review           - Create or Update your review
 *   POST /brokers/reviews/{id}/delete   - Delete your review
 */
@Controller
public class BrokerController {

    private final BrokerRepository brokerRepository;
    private final BrokerReviewService reviewService;
    private final CurrentUserService currentUserService;

    public BrokerController(BrokerRepository brokerRepository,
                            BrokerReviewService reviewService,
                            CurrentUserService currentUserService) {
        this.brokerRepository = brokerRepository;
        this.reviewService = reviewService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/brokers")
    public String directory(@RequestParam(required = false) String province,
                            @RequestParam(required = false) String q,
                            @RequestParam(required = false, defaultValue = "0") int minRating,
                            @RequestParam(required = false, defaultValue = "name") String sort,
                            Model model) {

        Province selected = Province.fromKey(province);
        List<Broker> brokers = selected == null
                ? brokerRepository.findAllByOrderByNameAsc()
                : brokerRepository.findByDistrictInOrderByNameAsc(District.inProvince(selected));

        if (q != null && !q.isBlank()) {
            String needle = q.trim().toLowerCase(Locale.ROOT);
            brokers = brokers.stream()
                    .filter(b -> (b.getName() + " " + b.getCompany() + " " + b.getDistrict().getLabel())
                            .toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
        }

        User viewer = currentUserService.get().orElse(null);
        List<BrokerCard> cards = reviewService.cards(brokers, viewer);

        int min = Math.max(0, Math.min(5, minRating));
        if (min > 0) {
            cards = cards.stream().filter(c -> c.getCount() > 0 && c.getAverage() >= min).toList();
        }
        if ("rating".equals(sort)) {
            cards = cards.stream()
                    .sorted(Comparator.comparingDouble(BrokerCard::getAverage).reversed()
                            .thenComparing(Comparator.comparingLong(BrokerCard::getCount).reversed()))
                    .toList();
        }

        model.addAttribute("cards", cards);
        model.addAttribute("provinces", Province.values());
        model.addAttribute("selectedProvince", selected == null ? "" : selected.getKey());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("minRating", min);
        model.addAttribute("sort", "rating".equals(sort) ? "rating" : "name");
        return "brokers";
    }

    @PostMapping("/brokers/{id}/review")
    public String review(@PathVariable Long id,
                         @RequestParam int rating,
                         @RequestParam(required = false) String comment,
                         RedirectAttributes redirectAttributes) {
        User user = currentUserService.get().orElseThrow();
        try {
            reviewService.save(user, id, rating, comment);
            redirectAttributes.addFlashAttribute("flash", "brokers.review.saved");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/brokers#broker-" + id;
    }

    @PostMapping("/brokers/reviews/{reviewId}/delete")
    public String deleteReview(@PathVariable Long reviewId, RedirectAttributes redirectAttributes) {
        User user = currentUserService.get().orElseThrow();
        reviewService.deleteOwn(user, reviewId);
        redirectAttributes.addFlashAttribute("flash", "brokers.review.deleted");
        return "redirect:/brokers";
    }
}
