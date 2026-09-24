package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.AdminUser;
import com.example.ceylonestate.model.RegularUser;
import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.UserRepository;
import com.example.ceylonestate.service.EmailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.ceylonestate.service.AvatarStorageService;
import java.io.IOException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AvatarStorageService avatarStorageService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           EmailService emailService, AvatarStorageService avatarStorageService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.avatarStorageService = avatarStorageService;
    }

    // ---------- Registration ----------

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") RegisterRequest form,
                            BindingResult result,
                            Model model) {

        if (userRepository.findByUsername(form.getUsername()).isPresent()) {
            result.rejectValue("username", "error.user", "Username already taken");
        }

        if (userRepository.findByEmail(form.getEmail()).isPresent()) {
            result.rejectValue("email", "error.user", "Email already registered");
        }

        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.user", "Passwords do not match");
        }

        if (result.hasErrors()) {
            return "register";
        }

        String hashed = passwordEncoder.encode(form.getPassword());

        User user = "ADMIN".equals(form.getAccountType())
                ? new AdminUser(form.getUsername(), form.getEmail(), hashed)
                : new RegularUser(form.getUsername(), form.getEmail(), hashed);

        user.setBackupEmail(form.getBackupEmail());
        user.setPhoneNumber(form.getPhoneNumber());

        userRepository.save(user);

        return "redirect:/login?registered";
    }

    // ---------- Login ----------

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        model.addAttribute("user", user);
        return "profile";
    }

    @GetMapping("/logout-confirm")
    public String showLogoutConfirm() {
        return "logout-confirm";
    }

    @PostMapping("/logout-confirm")
    public String confirmLogout(Authentication authentication,
                                 @RequestParam String password,
                                 HttpServletRequest request,
                                 Model model) {

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            model.addAttribute("error", "Incorrect password.");
            return "logout-confirm";
        }

        SecurityContextHolder.clearContext();
        request.getSession().invalidate();

        return "redirect:/login?logout";
    }

    @GetMapping("/profile/change-password")
    public String showChangePasswordForm() {
        return "change-password";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(Authentication authentication,
                                  @RequestParam String currentPassword,
                                  @RequestParam String newPassword,
                                  @RequestParam String confirmPassword,
                                  Model model) {

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            model.addAttribute("error", "Current password is incorrect.");
            return "change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "New passwords do not match.");
            return "change-password";
        }

        if (!newPassword.matches("^(?=.*[A-Z])(?=.*[0-9]).{8,}$")) {
            model.addAttribute("error", "New password must be at least 8 characters and include an uppercase letter and a number.");
            return "change-password";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return "redirect:/profile?passwordChanged";
    }

    @GetMapping("/profile/edit")
    public String showEditProfileForm(Authentication authentication, Model model) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();

        EditProfileRequest form = new EditProfileRequest();
        form.setUsername(user.getUsername());
        form.setEmail(user.getEmail());
        form.setBackupEmail(user.getBackupEmail());
        form.setPhoneNumber(user.getPhoneNumber());
        form.setGender(user.getGender());

        model.addAttribute("form", form);
        return "edit-profile";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(Authentication authentication,
                                 @Valid @ModelAttribute("form") EditProfileRequest form,
                                 BindingResult result,
                                 Model model) {

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();

        // Only flag "taken" if it belongs to a DIFFERENT user
        userRepository.findByUsername(form.getUsername()).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                result.rejectValue("username", "error.user", "Username already taken");
            }
        });
        userRepository.findByEmail(form.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                result.rejectValue("email", "error.user", "Email already registered");
            }
        });

        if (result.hasErrors()) {
            return "edit-profile";
        }

        user.setUsername(form.getUsername());
        user.setEmail(form.getEmail());
        user.setBackupEmail(form.getBackupEmail());
        user.setPhoneNumber(form.getPhoneNumber());
        user.setGender(form.getGender());
        userRepository.save(user);

        // Username may have changed, which invalidates the current login session -
        // safest to log the user out and have them log back in with the new username.
        SecurityContextHolder.clearContext();
        return "redirect:/login?profileUpdated";
    }

    @PostMapping("/profile/avatar")
    public String uploadAvatar(Authentication authentication,
                                @RequestParam("avatar") MultipartFile avatar,
                                RedirectAttributes redirectAttributes) {

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();

        try {
            String newFilename = avatarStorageService.store(avatar);

            // Clean up the old picture so we don't accumulate orphaned files on disk
            avatarStorageService.delete(user.getAvatarFilename());

            user.setAvatarFilename(newFilename);
            userRepository.save(user);
        } catch (IllegalArgumentException | IOException e) {
            redirectAttributes.addFlashAttribute("avatarError", e.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/profile/delete")
    public String deleteAccount(Authentication authentication, HttpServletRequest request) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();

        avatarStorageService.delete(user.getAvatarFilename());
        userRepository.delete(user);

        SecurityContextHolder.clearContext();
        request.getSession().invalidate();

        return "redirect:/?accountDeleted";
    }

    @GetMapping("/admin")
    public String adminDashboard(Authentication authentication, Model model) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("totalUsers", userRepository.count());
        return "admin";
    }

    // ---------- Forgot / reset password ----------

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam String email, Model model) {
        Optional<User> maybeUser = userRepository.findByEmail(email);

        if (maybeUser.isEmpty()) {
            // Don't reveal whether the email exists - just show the same message either way
            model.addAttribute("message", "If that email is registered, a reset link has been generated below.");
            return "forgot-password";
        }

        User user = maybeUser.get();
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        // Base URL is hardcoded for local development - update this if the site
        // ever moves to a custom domain or different port.
        String resetLink = "http://localhost:8080/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);

        model.addAttribute("message", "If that email is registered, a reset link has been sent to it. Check your inbox (and spam folder).");
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        Optional<User> maybeUser = userRepository.findByResetToken(token);

        if (maybeUser.isEmpty() || maybeUser.get().getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            model.addAttribute("expired", true);
            return "reset-password";
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam String token,
                                       @RequestParam String password,
                                       @RequestParam String confirmPassword,
                                       Model model) {

        Optional<User> maybeUser = userRepository.findByResetToken(token);

        if (maybeUser.isEmpty() || maybeUser.get().getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            model.addAttribute("expired", true);
            return "reset-password";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Passwords do not match");
            return "reset-password";
        }

        User user = maybeUser.get();
        user.setPassword(passwordEncoder.encode(password));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return "redirect:/login?resetSuccess";
    }

    /** Form-backing object for editing profile details (no password here - that goes through reset-password). */
    public static class EditProfileRequest {

        @NotBlank
        private String username;

        @NotBlank
        @Email
        private String email;

        private String backupEmail;
        private String phoneNumber;
        private String gender;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getBackupEmail() { return backupEmail; }
        public void setBackupEmail(String backupEmail) { this.backupEmail = backupEmail; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
    }

    /** Form-backing object for the registration page. */
    public static class RegisterRequest {

        @NotBlank
        private String username;

        @NotBlank
        @Email
        private String email;

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[0-9]).{8,}$",
                message = "Password must be at least 8 characters and include an uppercase letter and a number"
        )
        private String password;

        @NotBlank
        private String confirmPassword;

        private String backupEmail; // optional
        private String phoneNumber; // optional
        private String accountType = "USER"; // "USER" or "ADMIN"

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

        public String getBackupEmail() { return backupEmail; }
        public void setBackupEmail(String backupEmail) { this.backupEmail = backupEmail; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }
    }
}
