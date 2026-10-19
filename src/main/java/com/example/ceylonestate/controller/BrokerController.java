package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.Broker;
import com.example.ceylonestate.model.District;
import com.example.ceylonestate.model.Province;
import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.BrokerRepository;
import com.example.ceylonestate.service.BrokerReviewService;
import com.example.ceylonestate.service.BrokerReviewService.BrokerCard;
import com.example.ceylonestate.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The public mortgage broker directory: browse, search, filter by province and
 * by star rating, read other people's comments, and (signed in) rate a broker.
 *   GET  /brokers                       - Read (directory: small cards, each opens the broker's page)
 *   GET  /brokers/{id}                  - Read (one broker's own page: details, reviews, message form)
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
                            @RequestParam(required = false) String district,
                            @RequestParam(required = false) String q,
                            @RequestParam(required = false, defaultValue = "0") int minRating,
                            @RequestParam(required = false, defaultValue = "name") String sort,
                            Model model) {

        Province selectedProvince = Province.fromKey(province);
        District selectedDistrict = parseDistrict(district);

        // FILTERS narrow the list down...
        List<Broker> brokers = brokerRepository.findAllByOrderByNameAsc();
        if (selectedProvince != null) {
            brokers = brokers.stream().filter(b -> b.getProvince() == selectedProvince).toList();
        }
        if (selectedDistrict != null) {
            brokers = brokers.stream().filter(b -> b.getDistrict() == selectedDistrict).toList();
        }
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

        // ...SORT only changes the order of what is left.
        String sortKey = switch (sort) {
            case "rating", "province" -> sort;
            default -> "name";
        };
        Comparator<BrokerCard> byName = Comparator.comparing(c -> c.getBroker().getName(), String.CASE_INSENSITIVE_ORDER);
        cards = switch (sortKey) {
            case "rating" -> cards.stream()
                    .sorted(Comparator.comparingDouble(BrokerCard::getAverage).reversed()
                            .thenComparing(Comparator.comparingLong(BrokerCard::getCount).reversed())
                            .thenComparing(byName))
                    .toList();
            case "province" -> cards.stream()
                    .sorted(Comparator.<BrokerCard, Integer>comparing(c -> c.getBroker().getProvince().ordinal())
                            .thenComparing(c -> c.getBroker().getDistrict().getLabel())
                            .thenComparing(byName))
                    .toList();
            default -> cards.stream().sorted(byName).toList();
        };

        int activeFilters = (selectedProvince != null ? 1 : 0) + (selectedDistrict != null ? 1 : 0) + (min > 0 ? 1 : 0);

        model.addAttribute("cards", cards);
        model.addAttribute("provinces", Province.values());
        model.addAttribute("districts", District.values());
        model.addAttribute("selectedProvince", selectedProvince);
        model.addAttribute("selectedDistrict", selectedDistrict);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("minRating", min);
        model.addAttribute("sort", sortKey);
        model.addAttribute("activeFilters", activeFilters);
        return "brokers";
    }

    /** One broker's own page: full details, reviews, and the form to message them. */
    @GetMapping("/brokers/{id:\\d+}")
    public String profile(@PathVariable Long id, Model model) {
        Broker broker = brokerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        User viewer = currentUserService.get().orElse(null);
        BrokerCard card = reviewService.cards(List.of(broker), viewer).get(0);
        model.addAttribute("broker", broker);
        model.addAttribute("card", card);
        return "broker-detail";
    }

    private static District parseDistrict(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return District.valueOf(name.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
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
        return "redirect:/brokers/" + id;
    }

    @PostMapping("/brokers/reviews/{reviewId}/delete")
    public String deleteReview(@PathVariable Long reviewId,
                               @RequestParam(required = false) Long brokerId,
                               RedirectAttributes redirectAttributes) {
        User user = currentUserService.get().orElseThrow();
        reviewService.deleteOwn(user, reviewId);
        redirectAttributes.addFlashAttribute("flash", "brokers.review.deleted");
        return brokerId != null ? "redirect:/brokers/" + brokerId : "redirect:/brokers";
    }
}
