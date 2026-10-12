package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.Broker;
import com.example.ceylonestate.model.District;
import com.example.ceylonestate.model.Property;
import com.example.ceylonestate.model.TownDirectory;
import com.example.ceylonestate.repository.BrokerRepository;
import com.example.ceylonestate.repository.LocalInfoRepository;
import com.example.ceylonestate.service.PropertySearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * "Get Local Info": type a district, town or neighbourhood (or pick a
 * district) and see what the area is like - schools, transport, whether
 * pet-friendly rentals are common - plus the live listings there.
 */
@Controller
public class LocalInfoController {

    private final LocalInfoRepository localInfoRepository;
    private final PropertySearchService searchService;
    private final BrokerRepository brokerRepository;

    public LocalInfoController(LocalInfoRepository localInfoRepository,
                               PropertySearchService searchService,
                               BrokerRepository brokerRepository) {
        this.localInfoRepository = localInfoRepository;
        this.searchService = searchService;
        this.brokerRepository = brokerRepository;
    }

    @GetMapping("/local-info")
    public String localInfo(@RequestParam(required = false) String q,
                            @RequestParam(required = false) String district,
                            Model model) {

        String text = (district != null && !district.isBlank()) ? district : q;
        District selected = resolve(text);
        boolean searched = text != null && !text.isBlank();

        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("districts", District.values());
        model.addAttribute("searched", searched);

        if (selected != null) {
            List<Property> inArea = searchService.findLiveInDistrict(selected);
            List<Broker> brokers = brokerRepository
                    .findByDistrictInOrderByNameAsc(District.inProvince(selected.getProvince()));

            model.addAttribute("selected", selected);
            model.addAttribute("info", localInfoRepository.findByDistrict(selected).orElse(null));
            model.addAttribute("listings", inArea.stream().limit(6).toList());
            model.addAttribute("listingCount", inArea.size());
            model.addAttribute("petRentals", searchService.countPetFriendlyRentals(selected));
            model.addAttribute("brokers", brokers.stream().limit(3).toList());
        } else {
            // Nothing chosen (or nothing matched): show every district that has local info to browse.
            model.addAttribute("allInfo", localInfoRepository.findAllByOrderByDistrictAsc());
        }
        return "local-info";
    }

    /** District name first, then a known town ("Negombo"), then any listing whose city matches. */
    private District resolve(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        District byName = District.fromText(text);
        if (byName != null) {
            return byName;
        }
        District byTown = TownDirectory.find(text);
        if (byTown != null) {
            return byTown;
        }
        String needle = text.trim().toLowerCase();
        return searchService.findLive().stream()
                .filter(p -> p.getCity() != null && p.getCity().toLowerCase().contains(needle))
                .map(Property::getDistrict)
                .findFirst()
                .orElse(null);
    }
}
