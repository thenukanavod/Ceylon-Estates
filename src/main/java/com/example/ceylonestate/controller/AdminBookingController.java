package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.BookingStatus;
import com.example.ceylonestate.service.ViewingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin view of every viewing request: confirm, decline or delete. Limited to ADMIN by SecurityConfig. */
@Controller
@RequestMapping("/admin/bookings")
public class AdminBookingController {

    private final ViewingService viewingService;

    public AdminBookingController(ViewingService viewingService) {
        this.viewingService = viewingService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("bookings", viewingService.all());
        return "admin/bookings";
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return change(id, BookingStatus.CONFIRMED, "adminBookings.flash.confirmed", redirectAttributes);
    }

    @PostMapping("/{id}/decline")
    public String decline(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return change(id, BookingStatus.DECLINED, "adminBookings.flash.declined", redirectAttributes);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        viewingService.adminDelete(id);
        redirectAttributes.addFlashAttribute("flash", "adminBookings.flash.deleted");
        return "redirect:/admin/bookings";
    }

    private String change(Long id, BookingStatus status, String flash, RedirectAttributes redirectAttributes) {
        try {
            viewingService.setStatus(id, status);
            redirectAttributes.addFlashAttribute("flash", flash);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/admin/bookings";
    }
}
