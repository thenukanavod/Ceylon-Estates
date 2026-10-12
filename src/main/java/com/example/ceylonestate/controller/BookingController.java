package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.User;
import com.example.ceylonestate.service.CurrentUserService;
import com.example.ceylonestate.service.ViewingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

/**
 * Viewing bookings for signed-in buyers (CRUD on ViewingRequest):
 *   POST /my-bookings/book           - Create (the form on a property page)
 *   GET  /my-bookings                - Read
 *   POST /my-bookings/{id}/reschedule - Update
 *   POST /my-bookings/{id}/cancel    - Delete
 */
@Controller
public class BookingController {

    private final ViewingService viewingService;
    private final CurrentUserService currentUserService;

    public BookingController(ViewingService viewingService, CurrentUserService currentUserService) {
        this.viewingService = viewingService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/my-bookings")
    public String list(Model model) {
        User user = currentUserService.get().orElseThrow();
        model.addAttribute("bookings", viewingService.forUser(user));
        return "my-bookings";
    }

    @PostMapping("/my-bookings/book")
    public String book(@RequestParam Long propertyId,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime visitAt,
                       @RequestParam String phone,
                       @RequestParam(defaultValue = "") String note,
                       RedirectAttributes redirectAttributes) {
        User user = currentUserService.get().orElseThrow();
        try {
            viewingService.book(user, propertyId, visitAt, phone, note);
            redirectAttributes.addFlashAttribute("flash", "booking.flash.created");
            return "redirect:/my-bookings";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("flashError", e.getMessage());
            return "redirect:/properties/" + propertyId;
        }
    }

    @PostMapping("/my-bookings/{id}/reschedule")
    public String reschedule(@PathVariable Long id,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime visitAt,
                             RedirectAttributes redirectAttributes) {
        User user = currentUserService.get().orElseThrow();
        try {
            viewingService.reschedule(user, id, visitAt);
            redirectAttributes.addFlashAttribute("flash", "booking.flash.rescheduled");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/my-bookings";
    }

    @PostMapping("/my-bookings/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = currentUserService.get().orElseThrow();
        try {
            viewingService.cancel(user, id);
            redirectAttributes.addFlashAttribute("flash", "booking.flash.cancelled");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/my-bookings";
    }
}
