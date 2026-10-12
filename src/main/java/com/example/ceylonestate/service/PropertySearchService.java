package com.example.ceylonestate.service;

import com.example.ceylonestate.dto.PropertyFilter;
import com.example.ceylonestate.model.District;
import com.example.ceylonestate.model.ListingStatus;
import com.example.ceylonestate.model.Property;
import com.example.ceylonestate.repository.PropertyRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * All the "show buyers some listings" logic in one place: the homepage
 * carousel, the search page, the Completed Projects library and the
 * "similar listings" strip on a detail page.
 *
 * The database does the status filtering and ordering; the rest of the
 * filters run in memory with Java streams. That is plenty for a catalogue of
 * a few hundred listings and keeps every filter easy to read. For a much
 * bigger site you would push them into the query instead.
 */
@Service
public class PropertySearchService {

    private final PropertyRepository propertyRepository;

    public PropertySearchService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    /** Every listing buyers can currently see (for sale or for rent), newest first. */
    public List<Property> findLive() {
        return propertyRepository.findByStatusInOrderByListingDateDescIdDesc(ListingStatus.LIVE);
    }

    /** Sold and rented listings, newest first - the Completed Projects library. */
    public List<Property> findCompleted() {
        return propertyRepository.findByStatusInOrderByListingDateDescIdDesc(ListingStatus.COMPLETED);
    }

    /** The newest {@code perType} listings of each type, for the homepage carousel tabs. */
    public List<Property> latestPerType(int perType) {
        List<Property> live = findLive();
        List<Property> result = new ArrayList<>();
        for (String type : List.of("land", "house", "apartment")) {
            live.stream().filter(p -> p.getTypeKey().equals(type)).limit(perType).forEach(result::add);
        }
        return result;
    }

    /** Applies the search page's filters and sort order to the live listings. */
    public List<Property> search(PropertyFilter filter) {
        List<Property> results = new ArrayList<>();
        for (Property p : findLive()) {
            if (matches(p, filter)) {
                results.add(p);
            }
        }

        String sort = filter.getSort() == null ? "newest" : filter.getSort();
        switch (sort) {
            case "priceAsc" -> results.sort(Comparator.comparingDouble(Property::getTotalPrice));
            case "priceDesc" -> results.sort(Comparator.comparingDouble(Property::getTotalPrice).reversed());
            default -> { /* already newest-first from the database */ }
        }
        return results;
    }

    /** Up to {@code limit} other live listings in the same district (or, failing that, of the same type). */
    public List<Property> similarTo(Property property, int limit) {
        List<Property> others = findLive().stream()
                .filter(p -> !p.getId().equals(property.getId()))
                .toList();

        List<Property> result = new ArrayList<>();
        others.stream().filter(p -> p.getDistrict() == property.getDistrict()).limit(limit).forEach(result::add);
        if (result.size() < limit) {
            others.stream()
                    .filter(p -> p.getTypeKey().equals(property.getTypeKey()) && !result.contains(p))
                    .limit(limit - result.size())
                    .forEach(result::add);
        }
        return result;
    }

    /** How many pet-friendly rentals are on the market in a district right now. */
    public long countPetFriendlyRentals(District district) {
        return findLive().stream()
                .filter(p -> p.getDistrict() == district && p.isForRent() && p.isPetFriendly())
                .count();
    }

    /** Live listings in one district, newest first. */
    public List<Property> findLiveInDistrict(District district) {
        return findLive().stream().filter(p -> p.getDistrict() == district).toList();
    }

    // ------------------------------------------------------------------

    private boolean matches(Property p, PropertyFilter f) {
        Predicate<Property> all = x -> true;

        if (notBlank(f.getType())) {
            all = all.and(x -> x.getTypeKey().equalsIgnoreCase(f.getType().trim()));
        }

        if ("rent".equalsIgnoreCase(f.getListingType())) {
            all = all.and(Property::isForRent);
        } else if ("sale".equalsIgnoreCase(f.getListingType())) {
            all = all.and(x -> !x.isForRent());
        }

        District district = District.fromText(f.getDistrict());
        if (district != null) {
            all = all.and(x -> x.getDistrict() == district);
        }

        if (notBlank(f.getQ())) {
            String needle = f.getQ().trim().toLowerCase(Locale.ROOT);
            all = all.and(x -> haystack(x).contains(needle));
        }

        if (f.getMinPriceM() != null && f.getMinPriceM() > 0) {
            double min = f.getMinPriceM() * 1_000_000;
            all = all.and(x -> x.getTotalPrice() >= min);
        }
        if (f.getMaxPriceM() != null && f.getMaxPriceM() > 0) {
            double max = f.getMaxPriceM() * 1_000_000;
            all = all.and(x -> x.getTotalPrice() <= max);
        }

        int beds = atLeast(f.getBeds());
        if (beds > 0) {
            all = all.and(x -> x.getBedroomCount() >= beds);
        }
        int baths = atLeast(f.getBaths());
        if (baths > 0) {
            all = all.and(x -> x.getBathroomCount() >= baths);
        }

        if (f.isPet()) {
            all = all.and(Property::isPetFriendly);
        }

        return all.test(p);
    }

    /** Everything a free-text search should be able to hit, lower-cased. */
    private String haystack(Property p) {
        return String.join(" ",
                nullToEmpty(p.getTitle()),
                nullToEmpty(p.getCity()),
                nullToEmpty(p.getAddress()),
                nullToEmpty(p.getDescription()),
                p.getDistrict() == null ? "" : p.getDistrict().getLabel(),
                p.getTypeKey()).toLowerCase(Locale.ROOT);
    }

    /** "3" -> 3, "5+" -> 5, anything else -> 0 (meaning "don't filter"). */
    private int atLeast(String chip) {
        if (chip == null) {
            return 0;
        }
        String digits = chip.replace("+", "").trim();
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
