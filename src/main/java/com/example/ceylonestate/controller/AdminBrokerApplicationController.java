package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.ApplicationStatus;
import com.example.ceylonestate.model.Broker;
import com.example.ceylonestate.model.BrokerApplication;
import com.example.ceylonestate.repository.BrokerApplicationRepository;
import com.example.ceylonestate.repository.BrokerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin review of "apply to be a broker" requests. Approving adds the person to the public brokers list. */
@Controller
@RequestMapping("/admin/broker-applications")
public class AdminBrokerApplicationController {

    private final BrokerApplicationRepository applications;
    private final BrokerRepository brokers;

    public AdminBrokerApplicationController(BrokerApplicationRepository applications, BrokerRepository brokers) {
        this.applications = applications;
        this.brokers = brokers;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("applications", applications.findAllByOrderByCreatedAtDesc());
        return "admin/broker-applications";
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public String approve(@PathVariable Long id, RedirectAttributes redirect) {
        BrokerApplication app = find(id);
        if (app.getStatus() != ApplicationStatus.APPROVED) {
            Broker broker = app.toBroker();
            // The applicant can sign in as this broker (inbox) unless that account already has a broker profile.
            if (brokers.findByAccount(app.getApplicant()).isEmpty()) {
                broker.linkAccount(app.getApplicant());
            }
            brokers.save(broker);
            app.approve();
            applications.save(app);
        }
        redirect.addFlashAttribute("flash", "adminApps.flash.approved");
        return "redirect:/admin/broker-applications";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes redirect) {
        BrokerApplication app = find(id);
        app.reject();
        applications.save(app);
        redirect.addFlashAttribute("flash", "adminApps.flash.rejected");
        return "redirect:/admin/broker-applications";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        applications.delete(find(id));
        redirect.addFlashAttribute("flash", "adminApps.flash.deleted");
        return "redirect:/admin/broker-applications";
    }

    private BrokerApplication find(Long id) {
        return applications.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
