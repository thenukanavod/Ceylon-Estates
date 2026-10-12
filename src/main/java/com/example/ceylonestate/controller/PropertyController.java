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
}
