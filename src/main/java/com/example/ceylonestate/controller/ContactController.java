package com.example.ceylonestate.controller;

import com.example.ceylonestate.dto.ContactForm;
import com.example.ceylonestate.model.ContactMessage;
import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.ContactMessageRepository;
import com.example.ceylonestate.service.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The public "Contact us" page (the homepage "Get in Touch" button).
 * Messages land in the admin inbox at /admin/messages.
 */
@Controller
public class ContactController {

    private static final String LAST_SENT = "contact.lastSentAt";
    private static final long MIN_SECONDS_BETWEEN_MESSAGES = 20;

    private final ContactMessageRepository repository;
    private final CurrentUserService currentUserService;

    public ContactController(ContactMessageRepository repository, CurrentUserService currentUserService) {
        this.repository = repository;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/contact")
    public String show(@RequestParam(required = false) String subject, Model model) {
        ContactForm form = new ContactForm();
        // Signed-in visitors do not have to retype their details.
        User user = currentUserService.get().orElse(null);
        if (user != null) {
            form.setName(user.getUsername());
            form.setEmail(user.getEmail());
            form.setPhone(user.getPhoneNumber());
        }
        if (subject != null && !subject.isBlank()) {
            form.setSubject(subject.length() > 150 ? subject.substring(0, 150) : subject);
        }
        model.addAttribute("form", form);
        return "contact";
    }

    @PostMapping("/contact")
    public String submit(@Valid @ModelAttribute("form") ContactForm form,
                         BindingResult result,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        // Bots fill the hidden "website" field; pretend it worked and drop the message.
        if (form.getWebsite() != null && !form.getWebsite().isBlank()) {
            redirectAttributes.addFlashAttribute("flash", "contact.flash.sent");
            return "redirect:/contact";
        }

        Object last = session.getAttribute(LAST_SENT);
        if (last instanceof Long lastMillis && System.currentTimeMillis() - lastMillis < MIN_SECONDS_BETWEEN_MESSAGES * 1000) {
            result.reject("contact.tooFast", "Please wait a few seconds before sending another message.");
        }

        if (result.hasErrors()) {
            return "contact";
        }

        repository.save(new ContactMessage(
                form.getName().trim(),
                form.getEmail().trim(),
                form.getPhone() == null || form.getPhone().isBlank() ? null : form.getPhone().trim(),
                form.getSubject().trim(),
                form.getMessage().trim()));
        session.setAttribute(LAST_SENT, System.currentTimeMillis());

        redirectAttributes.addFlashAttribute("flash", "contact.flash.sent");
        return "redirect:/contact";
    }
}
