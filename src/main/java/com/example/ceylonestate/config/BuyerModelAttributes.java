package com.example.ceylonestate.config;

import com.example.ceylonestate.service.CurrentUserService;
import com.example.ceylonestate.service.SavedPropertyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Adds two small things every page with listing cards needs:
 *   savedIds - the ids of the properties the signed-in buyer has saved (empty if signed out),
 *              so each card can show a filled or empty heart;
 *   minVisit - the earliest time the booking form should allow, in the format the
 *              browser's date-time picker expects.
 */
@ControllerAdvice
public class BuyerModelAttributes {

    private static final DateTimeFormatter PICKER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final CurrentUserService currentUserService;
    private final SavedPropertyService savedPropertyService;

    public BuyerModelAttributes(CurrentUserService currentUserService, SavedPropertyService savedPropertyService) {
        this.currentUserService = currentUserService;
        this.savedPropertyService = savedPropertyService;
    }

    /**
     * Makes sure the visitor has a session BEFORE the page starts to render.
     * The heart buttons are POST forms, and Spring Security puts a hidden CSRF token
     * in each one. For a first-time visitor that token needs a new session, and a session
     * cannot be created once the browser has already received the first part of a long page
     * (the page then stops half-way and the browser reports ERR_INCOMPLETE_CHUNKED_ENCODING).
     */
    @ModelAttribute
    public void ensureSession(HttpServletRequest request) {
        request.getSession(true);
    }

    @ModelAttribute("savedIds")
    public Set<Long> savedIds() {
        return currentUserService.get()
                .map(savedPropertyService::savedIds)
                .orElse(Set.of());
    }

    @ModelAttribute("minVisit")
    public String minVisit() {
        return LocalDateTime.now().plusHours(1).format(PICKER);
    }
}
