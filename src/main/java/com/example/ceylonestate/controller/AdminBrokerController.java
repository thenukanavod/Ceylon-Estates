package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.Broker;
import com.example.ceylonestate.model.District;
import com.example.ceylonestate.repository.BrokerRepository;
import com.example.ceylonestate.service.BrokerPhotoStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

/**
 * Admin CRUD for the broker database: list, add, edit, delete.
 * (Everything under /admin/** is restricted to the ADMIN role in SecurityConfig.)
 */
@Controller
@RequestMapping("/admin/brokers")
public class AdminBrokerController {

    private final BrokerRepository brokerRepository;
    private final BrokerPhotoStorageService photoStorage;

    public AdminBrokerController(BrokerRepository brokerRepository, BrokerPhotoStorageService photoStorage) {
        this.brokerRepository = brokerRepository;
        this.photoStorage = photoStorage;
    }

    // ---------- Read ----------

    @GetMapping
    public String list(Model model) {
        model.addAttribute("brokers", brokerRepository.findAllByOrderByNameAsc());
        return "admin/brokers";
    }

    // ---------- Create ----------

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        prepareForm(model, Broker.blank(), false);
        return "admin/broker-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("broker") Broker broker,
                         BindingResult result,
                         @RequestParam(value = "photo", required = false) MultipartFile photo,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        broker.setPhotoUrl(null); // never trust a posted photo URL
        if (!result.hasErrors() && photo != null && !photo.isEmpty()) {
            try {
                broker.setPhotoUrl(photoStorage.store(photo));
            } catch (IllegalArgumentException | IOException e) {
                result.reject("photo", e.getMessage());
            }
        }
        if (result.hasErrors()) {
            prepareForm(model, broker, false);
            return "admin/broker-form";
        }
        brokerRepository.save(broker);
        redirectAttributes.addFlashAttribute("flash", "admin.flash.brokerSaved");
        return "redirect:/admin/brokers";
    }

    // ---------- Update ----------

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        prepareForm(model, find(id), true);
        return "admin/broker-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("broker") Broker form,
                         BindingResult result,
                         @RequestParam(value = "photo", required = false) MultipartFile photo,
                         @RequestParam(value = "removePhoto", defaultValue = "false") boolean removePhoto,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Broker existing = find(id);
        String newPhotoUrl = null;
        if (!result.hasErrors() && photo != null && !photo.isEmpty()) {
            try {
                newPhotoUrl = photoStorage.store(photo);
            } catch (IllegalArgumentException | IOException e) {
                result.reject("photo", e.getMessage());
            }
        }
        if (result.hasErrors()) {
            // Keep the id on the object the page re-renders so the form still posts to the right place.
            form = existing;
            prepareForm(model, form, true);
            return "admin/broker-form";
        }

        // Copy the submitted values onto the stored broker (never trust a posted id).
        existing.setName(form.getName());
        existing.setEmail(form.getEmail());
        existing.setPhone(form.getPhone());
        existing.setCompany(form.getCompany());
        existing.setLicenseNumber(form.getLicenseNumber());
        existing.setDistrict(form.getDistrict());
        existing.setBio(form.getBio());

        // Photo: a new upload replaces the old file; the tick box removes it.
        if (newPhotoUrl != null) {
            photoStorage.delete(existing.getPhotoUrl());
            existing.setPhotoUrl(newPhotoUrl);
        } else if (removePhoto) {
            photoStorage.delete(existing.getPhotoUrl());
            existing.setPhotoUrl(null);
        }
        brokerRepository.save(existing);

        redirectAttributes.addFlashAttribute("flash", "admin.flash.brokerSaved");
        return "redirect:/admin/brokers";
    }

    // ---------- Delete only the photo (the "Delete photo" button on the edit form) ----------

    @PostMapping("/{id}/photo/delete")
    public String deletePhoto(@PathVariable Long id) {
        Broker broker = find(id);
        photoStorage.delete(broker.getPhotoUrl());
        broker.setPhotoUrl(null);
        brokerRepository.save(broker);
        return "redirect:/admin/brokers/" + id + "/edit";
    }

    // ---------- Delete ----------

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Broker broker = find(id);
        photoStorage.delete(broker.getPhotoUrl());
        brokerRepository.delete(broker);
        redirectAttributes.addFlashAttribute("flash", "admin.flash.brokerDeleted");
        return "redirect:/admin/brokers";
    }

    private Broker find(Long id) {
        return brokerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void prepareForm(Model model, Broker broker, boolean editing) {
        model.addAttribute("broker", broker);
        model.addAttribute("districts", District.values());
        model.addAttribute("editing", editing);
    }
}
