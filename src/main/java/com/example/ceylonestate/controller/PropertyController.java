package com.example.ceylonestate.controller;

import com.example.ceylonestate.dto.PropertyFilter;
import com.example.ceylonestate.model.Broker;
import com.example.ceylonestate.model.District;
import com.example.ceylonestate.model.Property;
import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.BrokerRepository;
import com.example.ceylonestate.repository.LocalInfoRepository;
import com.example.ceylonestate.repository.PropertyRepository;
import com.example.ceylonestate.service.CurrentUserService;
import com.example.ceylonestate.service.PropertySearchService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

/**
 * The public, buyer-facing pages: search results, a single listing's detail
 * page, and the Completed Projects library.
 */
@Controller
public class PropertyController {

    private final PropertyRepository propertyRepository;
    private final PropertySearchService searchService;
    private final LocalInfoRepository localInfoRepository;
    private final BrokerRepository brokerRepository;
    private final CurrentUserService currentUserService;

    public PropertyController(PropertyRepository propertyRepository,
                              PropertySearchService searchService,
                              LocalInfoRepository localInfoRepository,
                              BrokerRepository brokerRepository,
                              CurrentUserService currentUserService) {
        this.propertyRepository = propertyRepository;
        this.searchService = searchService;
        this.localInfoRepository = localInfoRepository;
        this.brokerRepository = brokerRepository;
        this.currentUserService = currentUserService;
    }

    /** Search / browse. The homepage hero search submits here as a GET form. */
    @GetMapping("/properties")
    public String list(@ModelAttribute("filter") PropertyFilter filter,
                       BindingResult ignoredBindingErrors,
                       Model model) {
        // BindingResult is declared so that an odd value in the URL never turns into an error page.
        model.addAttribute("results", searchService.search(filter));
        model.addAttribute("districts", District.values());
        return "properties";
    }

    /** One listing: gallery, key facts, contact details, local info and nearby brokers. */
    @GetMapping("/properties/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        User viewer = currentUserService.get().orElse(null);
        boolean isOwner = property.isOwnedBy(viewer);
        boolean isAdmin = viewer != null && "ADMIN".equals(viewer.getRole());

        // Pending and rejected listings are only visible to the seller and to admins.
        boolean publiclyVisible = property.getStatus().isLive() || property.getStatus().isCompleted();
        if (!publiclyVisible && !isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // Count a view only for buyers looking at a live listing.
        if (!isOwner && !isAdmin && property.getStatus().isLive()) {
            propertyRepository.incrementViewCount(id);
        }

        model.addAttribute("property", property);
        model.addAttribute("localInfo", localInfoRepository.findByDistrict(property.getDistrict()).orElse(null));
        model.addAttribute("petRentals", searchService.countPetFriendlyRentals(property.getDistrict()));
        model.addAttribute("brokers", nearbyBrokers(property));
        model.addAttribute("allBrokers", brokerRepository.findAllByOrderByNameAsc()); // "share with a broker" list
        model.addAttribute("similar", searchService.similarTo(property, 3));
        model.addAttribute("canManage", isOwner || isAdmin);
        return "property-detail";
    }

    /** The Completed Projects library: everything that has sold or been rented out. */
    @GetMapping("/projects")
    public String projects(Model model) {
        List<Property> completed = searchService.findCompleted();
        model.addAttribute("projects", completed);
        model.addAttribute("count", completed.size());
        return "projects";
    }

    /** Mortgage brokers in the same province as the property, same-district ones first. */
    private List<Broker> nearbyBrokers(Property property) {
        List<District> districts = District.inProvince(property.getDistrict().getProvince());
        return brokerRepository.findByDistrictInOrderByNameAsc(districts).stream()
                .sorted(Comparator.comparing((Broker b) -> b.getDistrict() != property.getDistrict()))
                .limit(4)
                .toList();
    }

    /**
     * Search-as-you-type suggestions for the main search bar: districts, known
     * towns, cities and listing titles that match what has been typed so far.
     * Names that start with the typed text come first; at most 8 are returned.
     */
    @GetMapping("/properties/suggest")
    @org.springframework.web.bind.annotation.ResponseBody
    public List<java.util.Map<String, String>> suggest(
            @org.springframework.web.bind.annotation.RequestParam(name = "q", required = false) String q) {
        List<java.util.Map<String, String>> out = new java.util.ArrayList<>();
        if (q == null || q.isBlank() || q.length() > 40) {
            return out;
        }
        String needle = q.trim().toLowerCase();
        java.util.Map<String, java.util.Map<String, String>> starts = new java.util.LinkedHashMap<>();
        java.util.Map<String, java.util.Map<String, String>> contains = new java.util.LinkedHashMap<>();

        for (District d : District.values()) {
            addSuggestion(needle, d.getLabel(), "District", starts, contains);
        }
        com.example.ceylonestate.model.TownDirectory.all().forEach((town, d) ->
                addSuggestion(needle, titleCase(town), d.getLabel() + " District", starts, contains));
        for (Property p : searchService.findLive()) {
            if (p.getCity() != null && !p.getCity().isBlank()) {
                addSuggestion(needle, p.getCity(), "City", starts, contains);
            }
            if (p.getTitle() != null) {
                addSuggestion(needle, p.getTitle(), "Listing", starts, contains);
            }
        }
        out.addAll(starts.values());
        out.addAll(contains.values());
        return out.size() > 8 ? out.subList(0, 8) : out;
    }

    private static void addSuggestion(String needle, String name, String hint,
                                      java.util.Map<String, java.util.Map<String, String>> starts,
                                      java.util.Map<String, java.util.Map<String, String>> contains) {
        String lower = name.toLowerCase();
        if (starts.containsKey(lower) || contains.containsKey(lower)) {
            return;
        }
        java.util.Map<String, String> item = new java.util.LinkedHashMap<>();
        item.put("text", name);
        item.put("hint", hint);
        if (lower.startsWith(needle)) {
            starts.put(lower, item);
        } else if (lower.contains(needle)) {
            contains.put(lower, item);
        }
    }

    private static String titleCase(String text) {
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (char c : text.toCharArray()) {
            sb.append(up ? Character.toUpperCase(c) : c);
            up = c == ' ' || c == '-';
        }
        return sb.toString();
    }
}
