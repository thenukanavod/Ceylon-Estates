package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.User;
import com.example.ceylonestate.service.CurrentUserService;
import com.example.ceylonestate.service.SavedPropertyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

/**
 * Saved properties (CRUD on SavedProperty). Signed-in users only (SecurityConfig).
 *   POST /saved/{propertyId}/toggle - Create (heart on) or Delete (heart off)
 *   GET  /saved                     - Read the list
 *   POST /saved/{id}/note           - Update the private note
 *   POST /saved/{id}/remove         - Delete
 */
@Controller
public class SavedController {

    private final SavedPropertyService savedService;
    private final CurrentUserService currentUserService;

    public SavedController(SavedPropertyService savedService, CurrentUserService currentUserService) {
        this.savedService = savedService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/saved")
    public String list(Model model) {
        User user = currentUserService.get().orElseThrow();
        model.addAttribute("items", savedService.list(user));
        return "saved";
    }

    @PostMapping("/saved/{propertyId}/toggle")
    public Object toggle(@PathVariable Long propertyId, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        User user = currentUserService.get().orElseThrow();
        // The heart buttons call this with JavaScript (saved.js) so the page does not reload.
        boolean ajax = "fetch".equals(request.getHeader("X-Requested-With"));
        try {
            boolean nowSaved = savedService.toggle(user, propertyId);
            if (ajax) {
                return ResponseEntity.ok(nowSaved ? "saved" : "removed");
            }
            redirectAttributes.addFlashAttribute("flash", nowSaved ? "saved.flash.added" : "saved.flash.removed");
        } catch (IllegalArgumentException e) {
            if (ajax) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            redirectAttributes.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:" + backTo(request);
    }

    @PostMapping("/saved/{id}/note")
    public String note(@PathVariable Long id, @RequestParam(defaultValue = "") String note,
                       RedirectAttributes redirectAttributes) {
        User user = currentUserService.get().orElseThrow();
        try {
            savedService.updateNote(user, id, note);
            redirectAttributes.addFlashAttribute("flash", "saved.flash.noteSaved");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/saved";
    }

    @PostMapping("/saved/{id}/remove")
    public String remove(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = currentUserService.get().orElseThrow();
        savedService.remove(user, id);
        redirectAttributes.addFlashAttribute("flash", "saved.flash.removed");
        return "redirect:/saved";
    }

    /** Send the user back to the page they clicked the heart on (same site only), else to the listings. */
    private String backTo(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                URI uri = URI.create(referer);
                if (request.getServerName().equalsIgnoreCase(uri.getHost()) && uri.getRawPath() != null) {
                    String path = uri.getRawPath();
                    String context = request.getContextPath();
                    if (!context.isEmpty() && path.startsWith(context)) {
                        path = path.substring(context.length());
                    }
                    if (path.startsWith("/") && !path.startsWith("//")) {
                        return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // a malformed Referer header - fall through to the default
            }
        }
        return "/properties";
    }
}
