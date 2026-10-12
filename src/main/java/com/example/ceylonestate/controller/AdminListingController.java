package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.ListingStatus;
import com.example.ceylonestate.model.Property;
import com.example.ceylonestate.repository.PropertyRepository;
import com.example.ceylonestate.service.ListingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin review of seller submissions. Everything under /admin/** is already
 * limited to the ADMIN role in SecurityConfig.
 */
@Controller
@RequestMapping("/admin/listings")
public class AdminListingController {

    private final PropertyRepository propertyRepository;
    private final ListingService listingService;

    public AdminListingController(PropertyRepository propertyRepository, ListingService listingService) {
        this.propertyRepository = propertyRepository;
        this.listingService = listingService;
    }

    @GetMapping
    public String overview(Model model) {
        model.addAttribute("pending", propertyRepository.findByStatusOrderByIdAsc(ListingStatus.PENDING));
        model.addAttribute("all", propertyRepository.findAllByOrderByIdDesc());
        return "admin/listings";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        listingService.approve(find(id));
        redirectAttributes.addFlashAttribute("flash", "admin.flash.approved");
        return "redirect:/admin/listings";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         RedirectAttributes redirectAttributes) {
        listingService.reject(find(id), reason);
        redirectAttributes.addFlashAttribute("flash", "admin.flash.rejected");
        return "redirect:/admin/listings";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        listingService.delete(find(id));
        redirectAttributes.addFlashAttribute("flash", "admin.flash.deleted");
        return "redirect:/admin/listings";
    }

    private Property find(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
