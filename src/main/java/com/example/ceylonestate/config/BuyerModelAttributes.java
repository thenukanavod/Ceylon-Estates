package com.example.ceylonestate.config;

import com.example.ceylonestate.service.CurrentUserService;
import com.example.ceylonestate.service.SavedPropertyService;
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
