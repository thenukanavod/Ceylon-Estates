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
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Search-as-you-type suggestions for the Get Local Info box. Returns up to 8
     * matches from three sources: district names, known towns, and the cities of
     * live listings. Names that start with the typed text come first.
     */
    @GetMapping("/local-info/suggest")
    @ResponseBody
    public List<Map<String, String>> suggest(@RequestParam(name = "q", required = false) String q) {
        List<Map<String, String>> out = new ArrayList<>();
        if (q == null || q.isBlank()) {
            return out;
        }
        String needle = q.trim().toLowerCase();
        if (needle.length() > 40) {
            return out;
        }
        Map<String, Map<String, String>> starts = new LinkedHashMap<>();
        Map<String, Map<String, String>> contains = new LinkedHashMap<>();

        for (District d : District.values()) {
            addMatch(needle, d.getLabel(), d.getLabel() + " District", starts, contains);
        }
        TownDirectory.all().forEach((town, d) ->
                addMatch(needle, titleCase(town), d.getLabel() + " District", starts, contains));
        searchService.findLive().stream()
                .map(Property::getCity)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .forEach(c -> addMatch(needle, c, "Listings available", starts, contains));

        out.addAll(starts.values());
        out.addAll(contains.values());
        return out.size() > 8 ? out.subList(0, 8) : out;
    }

    private static void addMatch(String needle, String name, String hint,
                                 Map<String, Map<String, String>> starts,
                                 Map<String, Map<String, String>> contains) {
        String lower = name.toLowerCase();
        if (starts.containsKey(lower) || contains.containsKey(lower)) {
            return;
        }
        Map<String, String> item = new LinkedHashMap<>();
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
