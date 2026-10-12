package com.example.ceylonestate.controller;

import com.example.ceylonestate.repository.BrokerRepository;
import com.example.ceylonestate.service.PropertySearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Renders the homepage using Thymeleaf (server-side HTML templates).
 * Visit: http://localhost:8080/
 *
 * The "Latest listings" carousel and the "Local mortgage brokers" row are
 * now filled from the database instead of being typed into the HTML.
 */
@Controller
public class HomeController {

    private static final int LISTINGS_PER_TYPE = 8;

    private final PropertySearchService searchService;
    private final BrokerRepository brokerRepository;

    public HomeController(PropertySearchService searchService, BrokerRepository brokerRepository) {
        this.searchService = searchService;
        this.brokerRepository = brokerRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("appName", "Ceylon Estates");
        model.addAttribute("listings", searchService.latestPerType(LISTINGS_PER_TYPE));
        model.addAttribute("brokers", brokerRepository.findAllByOrderByNameAsc());
        return "index"; // resolves to src/main/resources/templates/index.html
    }
}
