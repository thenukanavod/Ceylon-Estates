package com.example.ceylonestate.controller;

import com.example.ceylonestate.dto.PropertyFilter;
import com.example.ceylonestate.model.District;
import com.example.ceylonestate.model.Property;
import com.example.ceylonestate.service.PropertySearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * The three category pages: /lands, /houses and /apartments.
 *
 * Each one has its own page layout and its own filters (plot size and zoning
 * for land, bedrooms and garden for houses, floor area and lift for
 * apartments). They all reuse PropertySearchService - the only difference is
 * that the listing type is fixed here, so a visitor on the Lands page can never
 * see a house.
 *
 * The homepage "Explore more" button and the three big category photos link here.
 */
@Controller
public class CategoryController {

    private final PropertySearchService searchService;

    public CategoryController(PropertySearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/lands")
    public String lands(@ModelAttribute("filter") PropertyFilter filter,
                        BindingResult ignoredBindingErrors,
                        @RequestParam Map<String, String> allParams, Model model) {
        return category("land", "lands", filter, allParams, model);
    }

    @GetMapping("/houses")
    public String houses(@ModelAttribute("filter") PropertyFilter filter,
                         BindingResult ignoredBindingErrors,
                         @RequestParam Map<String, String> allParams, Model model) {
        return category("house", "houses", filter, allParams, model);
    }

    @GetMapping("/apartments")
    public String apartments(@ModelAttribute("filter") PropertyFilter filter,
                             BindingResult ignoredBindingErrors,
                             @RequestParam Map<String, String> allParams, Model model) {
        return category("apartment", "apartments", filter, allParams, model);
    }

    private String category(String typeKey, String view, PropertyFilter filter,
                            Map<String, String> allParams, Model model) {
        // The type is decided by the page, never by the URL.
        filter.setType(typeKey);
        List<Property> results = searchService.search(filter);
        model.addAttribute("results", results);
        model.addAttribute("districts", District.values());
        model.addAttribute("activeCategory", typeKey);
        // true after a search, so the page can keep the filter panel open
        model.addAttribute("filtersApplied", !allParams.isEmpty());
        return view;
    }
}
