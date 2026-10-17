package com.example.ceylonestate.controller;

import com.example.ceylonestate.dto.BrokerApplicationForm;
import com.example.ceylonestate.model.ApplicationStatus;
import com.example.ceylonestate.model.BrokerApplication;
import com.example.ceylonestate.model.District;
import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.BrokerApplicationRepository;
import com.example.ceylonestate.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * "Apply to be a broker": a signed-in user sends their details; an admin reviews them
 * under /admin/broker-applications. (Login is enforced by SecurityConfig's anyRequest().authenticated().)
 */
@Controller
@RequestMapping("/brokers/apply")
public class BrokerApplicationController {

    private final BrokerApplicationRepository applications;
    private final CurrentUserService currentUser;

    public BrokerApplicationController(BrokerApplicationRepository applications, CurrentUserService currentUser) {
        this.applications = applications;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String showForm(Model model) {
        User user = currentUser.get().orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        if (!model.containsAttribute("form")) {
            BrokerApplicationForm form = new BrokerApplicationForm();
            form.setName(user.getUsername());
            form.setEmail(user.getEmail());
            model.addAttribute("form", form);
        }
        prepare(model, user);
        return "broker-apply";
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("form") BrokerApplicationForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        User user = currentUser.get().orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        if (applications.existsByApplicantAndStatus(user, ApplicationStatus.PENDING)) {
            result.reject("pending", "You already have an application waiting for review.");
        }
        if (result.hasErrors()) {
            prepare(model, user);
            return "broker-apply";
        }
        applications.save(new BrokerApplication(user, form.getName(), form.getEmail(), form.getPhone(),
                form.getCompany(), form.getLicenseNumber(), form.getDistrict(), form.getBio()));
        redirect.addFlashAttribute("flash", "brokerApply.sent");
        return "redirect:/brokers/apply";
    }

    private void prepare(Model model, User user) {
        model.addAttribute("districts", District.values());
        model.addAttribute("myApplications", applications.findByApplicantOrderByCreatedAtDesc(user));
    }
}
