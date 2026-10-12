package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.District;
import com.example.ceylonestate.model.LocalInfo;
import com.example.ceylonestate.repository.LocalInfoRepository;
import com.example.ceylonestate.service.LocalInfoCsvService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.nio.charset.StandardCharsets;

/** Admin CRUD for the per-district local information. Restricted to ADMIN via /admin/** in SecurityConfig. */
@Controller
@RequestMapping("/admin/local-info")
public class AdminLocalInfoController {

    private final LocalInfoRepository localInfoRepository;
    private final LocalInfoCsvService csvService;

    public AdminLocalInfoController(LocalInfoRepository localInfoRepository, LocalInfoCsvService csvService) {
        this.localInfoRepository = localInfoRepository;
        this.csvService = csvService;
    }

    // ---------- CSV import ----------

    @GetMapping("/import")
    public String showImport() {
        return "admin/local-info-import";
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=local-info-template.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvService.templateCsv().getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/import")
    public String importCsv(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            LocalInfoCsvService.ImportResult result = csvService.importCsv(file);
            redirectAttributes.addFlashAttribute("created", result.created());
            redirectAttributes.addFlashAttribute("updated", result.updated());
            redirectAttributes.addFlashAttribute("importErrors", result.errors());
        } catch (IllegalArgumentException | IOException e) {
            redirectAttributes.addFlashAttribute("importFailed", e.getMessage());
        }
        return "redirect:/admin/local-info/import";
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", localInfoRepository.findAllByOrderByDistrictAsc());
        return "admin/local-info";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        prepareForm(model, new LocalInfo(), false);
        return "admin/local-info-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("info") LocalInfo info,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (info.getDistrict() != null && localInfoRepository.existsByDistrict(info.getDistrict())) {
            result.rejectValue("district", "duplicate", "This district already has local info - edit that entry instead.");
        }
        if (result.hasErrors()) {
            prepareForm(model, info, false);
            return "admin/local-info-form";
        }
        localInfoRepository.save(info);
        redirectAttributes.addFlashAttribute("flash", "admin.flash.infoSaved");
        return "redirect:/admin/local-info";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        prepareForm(model, find(id), true);
        return "admin/local-info-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("info") LocalInfo form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        LocalInfo existing = find(id);

        // Moving an entry onto a district that already has its own entry would break "one per district".
        if (form.getDistrict() != null && form.getDistrict() != existing.getDistrict()
                && localInfoRepository.existsByDistrict(form.getDistrict())) {
            result.rejectValue("district", "duplicate", "That district already has local info.");
        }
        if (result.hasErrors()) {
            prepareForm(model, existing, true);
            return "admin/local-info-form";
        }

        existing.setDistrict(form.getDistrict());
        existing.setSummary(form.getSummary());
        existing.setSchoolInfo(form.getSchoolInfo());
        existing.setTransportInfo(form.getTransportInfo());
        existing.setAmenitiesInfo(form.getAmenitiesInfo());
        existing.setSafetyInfo(form.getSafetyInfo());
        existing.setPetFriendlyRentalsCommon(form.isPetFriendlyRentalsCommon());
        existing.setPetFriendlyNotes(form.getPetFriendlyNotes());
        existing.setSchoolRating(form.getSchoolRating());
        existing.setTransportRating(form.getTransportRating());
        existing.setAmenitiesRating(form.getAmenitiesRating());
        existing.setSafetyRating(form.getSafetyRating());
        localInfoRepository.save(existing);

        redirectAttributes.addFlashAttribute("flash", "admin.flash.infoSaved");
        return "redirect:/admin/local-info";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        localInfoRepository.delete(find(id));
        redirectAttributes.addFlashAttribute("flash", "admin.flash.infoDeleted");
        return "redirect:/admin/local-info";
    }

    private LocalInfo find(Long id) {
        return localInfoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void prepareForm(Model model, LocalInfo info, boolean editing) {
        model.addAttribute("info", info);
        model.addAttribute("districts", District.values());
        model.addAttribute("editing", editing);
    }
}
