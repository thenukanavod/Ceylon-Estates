package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.UserRepository;
import com.example.ceylonestate.service.UserAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

/** Admin user management: see every account, make someone an admin (or not), delete accounts. */
@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserAdminService userAdminService;

    public AdminUserController(UserRepository userRepository, UserAdminService userAdminService) {
        this.userRepository = userRepository;
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Authentication authentication, Model model) {
        List<User> users = userRepository.findAllByOrderByUsernameAsc();
        if (q != null && !q.isBlank()) {
            String needle = q.trim().toLowerCase(Locale.ROOT);
            users = users.stream()
                    .filter(u -> u.getUsername().toLowerCase(Locale.ROOT).contains(needle)
                            || u.getEmail().toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
        }
        model.addAttribute("users", users);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("me", authentication.getName());
        return "admin/users";
    }

    // ---------- Create an admin account ----------

    @GetMapping("/new")
    public String showCreateAdmin() {
        return "admin/user-form";
    }

    @PostMapping("/new")
    public String createAdmin(@RequestParam String username, @RequestParam String email,
                              @RequestParam String password, @RequestParam String confirmPassword,
                              RedirectAttributes redirectAttributes) {
        try {
            userAdminService.createAdmin(username, email, password, confirmPassword);
            redirectAttributes.addFlashAttribute("flash", "adminUsers.new.flash.created");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            // Passwords are never sent back to the page; the other fields are kept.
            redirectAttributes.addFlashAttribute("flashError", e.getMessage());
            redirectAttributes.addFlashAttribute("formUsername", username);
            redirectAttributes.addFlashAttribute("formEmail", email);
            return "redirect:/admin/users/new";
        }
    }

    @PostMapping("/{id}/promote")
    public String promote(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userAdminService.promote(id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        redirectAttributes.addFlashAttribute("flash", "adminUsers.flash.promoted");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/demote")
    public String demote(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            userAdminService.demote(id, authentication.getName());
            redirectAttributes.addFlashAttribute("flash", "adminUsers.flash.demoted");
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("flashError", errorKey(e));
            return "redirect:/admin/users?blocked";
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            userAdminService.delete(id, authentication.getName());
            redirectAttributes.addFlashAttribute("flash", "adminUsers.flash.deleted");
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("flashError", errorKey(e));
            return "redirect:/admin/users?blocked";
        }
        return "redirect:/admin/users";
    }

    private String errorKey(IllegalStateException e) {
        return "self".equals(e.getMessage()) ? "adminUsers.err.self" : "adminUsers.err.lastAdmin";
    }
}
