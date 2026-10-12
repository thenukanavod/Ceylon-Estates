package com.example.ceylonestate.controller;

import com.example.ceylonestate.dto.ListingForm;
import com.example.ceylonestate.model.District;
import com.example.ceylonestate.model.Property;
import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.PropertyRepository;
import com.example.ceylonestate.service.CurrentUserService;
import com.example.ceylonestate.service.ListingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

/**
 * The seller side of the site (all pages need a login):
 *   /sell                      - submit a new listing (goes into the admin review queue)
 *   /my-listings               - see your own listings and their status
 *   /my-listings/{id}/edit     - change a listing (Update)
 *   /my-listings/{id}/complete - mark it sold / rented
 *   /my-listings/{id}/delete   - remove it (Delete)
 */
@Controller
public class SellController {

    private final ListingService listingService;
    private final PropertyRepository propertyRepository;
    private final CurrentUserService currentUserService;

    public SellController(ListingService listingService,
                          PropertyRepository propertyRepository,
                          CurrentUserService currentUserService) {
        this.listingService = listingService;
        this.propertyRepository = propertyRepository;
        this.currentUserService = currentUserService;
    }

    // ---------- Create ----------

    @GetMapping("/sell")
    public String showSellForm(Model model) {
        User user = requireUser();

        ListingForm form = new ListingForm();
        // Pre-fill the contact details from the account - the seller can change them.
        form.setContactName(user.getUsername());
        form.setContactEmail(user.getEmail());
        form.setContactPhone(user.getPhoneNumber());

        prepareForm(model, form, null);
        return "sell";
    }

    @PostMapping("/sell")
    public String submitListing(@Valid @ModelAttribute("form") ListingForm form,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        User user = requireUser();
        validateTypeSpecificFields(form, result);

        if (!result.hasErrors()) {
            try {
                listingService.create(form, user);
                redirectAttributes.addFlashAttribute("flash", "sell.flash.submitted");
                return "redirect:/my-listings";
            } catch (IllegalArgumentException | IOException e) {
                result.reject("upload", e.getMessage());
            }
        }

        prepareForm(model, form, null);
        return "sell";
    }

    // ---------- Read ----------

    @GetMapping("/my-listings")
    public String myListings(Model model) {
        User user = requireUser();
        model.addAttribute("listings", propertyRepository.findByOwnerOrderByIdDesc(user));
        return "my-listings";
    }

    // ---------- Update ----------

    @GetMapping("/my-listings/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Property property = loadManageable(id);
        prepareForm(model, property.toForm(), property);
        return "sell";
    }

    @PostMapping("/my-listings/{id}/edit")
    public String saveEdit(@PathVariable Long id,
                           @Valid @ModelAttribute("form") ListingForm form,
                           BindingResult result,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Property property = loadManageable(id);

        // The type of an existing listing can't be changed - ignore whatever the form says.
        form.setType(property.getTypeKey());
        validateTypeSpecificFields(form, result);

        if (!result.hasErrors()) {
            try {
                listingService.update(property, form);
                redirectAttributes.addFlashAttribute("flash", "sell.flash.updated");
                return "redirect:/my-listings";
            } catch (IllegalArgumentException | IOException e) {
                result.reject("upload", e.getMessage());
            }
        }

        prepareForm(model, form, property);
        return "sell";
    }

    @PostMapping("/my-listings/{id}/duplicate")
    public String duplicate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Property copy = listingService.duplicate(loadManageable(id));
        redirectAttributes.addFlashAttribute("flash", "sell.flash.duplicated");
        return "redirect:/my-listings/" + copy.getId() + "/edit";
    }

    @PostMapping("/my-listings/{id}/complete")
    public String markCompleted(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        listingService.markCompleted(loadManageable(id));
        redirectAttributes.addFlashAttribute("flash", "sell.flash.completed");
        return "redirect:/my-listings";
    }

    // ---------- Delete ----------

    @PostMapping("/my-listings/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        listingService.delete(loadManageable(id));
        redirectAttributes.addFlashAttribute("flash", "sell.flash.deleted");
        return "redirect:/my-listings";
    }

    // ---------- helpers ----------

    private User requireUser() {
        return currentUserService.get()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /** Loads a listing that the logged-in user is allowed to change: their own, or any if they're an admin. */
    private Property loadManageable(Long id) {
        User user = requireUser();
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean isAdmin = "ADMIN".equals(user.getRole());
        if (!property.isOwnedBy(user) && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return property;
    }

    private void prepareForm(Model model, ListingForm form, Property existing) {
        model.addAttribute("form", form);
        model.addAttribute("districts", District.values());
        model.addAttribute("existing", existing); // null when creating a new listing
    }

    /**
     * Bean Validation covers the fields every listing has. The fields that
     * only matter for one type (bedrooms for a house, area for land...) are
     * checked here, because which ones are required depends on the type.
     */
    private void validateTypeSpecificFields(ListingForm form, BindingResult result) {
        if (form.getType() == null) {
            return;
        }
        switch (form.getType()) {
            case "house" -> {
                requireNumber(result, "bedrooms", form.getBedrooms());
                requireNumber(result, "bathrooms", form.getBathrooms());
                requirePositive(result, "floorAreaSqft", form.getFloorAreaSqft());
                requirePositive(result, "landSizePerches", form.getLandSizePerches());
            }
            case "apartment" -> {
                requireNumber(result, "bedrooms", form.getBedrooms());
                requireNumber(result, "bathrooms", form.getBathrooms());
                requirePositive(result, "floorAreaSqft", form.getFloorAreaSqft());
                requireNumber(result, "floorNumber", form.getFloorNumber());
            }
            case "land" -> {
                requirePositive(result, "areaPerches", form.getAreaPerches());
                if (form.isForRent()) {
                    result.rejectValue("forRent", "land.noRent", "Land can only be listed for sale.");
                }
            }
            default -> { /* an invalid type is already reported by the @Pattern on the field */ }
        }
    }

    private void requireNumber(BindingResult result, String field, Number value) {
        if (value == null && result.getFieldError(field) == null) {
            result.rejectValue(field, "required", "This field is required.");
        }
    }

    private void requirePositive(BindingResult result, String field, Double value) {
        if ((value == null || value <= 0) && result.getFieldError(field) == null) {
            result.rejectValue(field, "required", "Enter a value greater than zero.");
        }
    }
}
