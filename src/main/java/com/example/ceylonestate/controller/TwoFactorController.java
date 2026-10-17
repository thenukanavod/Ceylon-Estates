package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.UserRepository;
import com.example.ceylonestate.service.EmailService;
import com.example.ceylonestate.service.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Iterator;

@Controller
public class TwoFactorController {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final UserDetailsServiceImpl userDetailsService;
    private final RememberMeServices rememberMeServices;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public TwoFactorController(UserRepository userRepository, EmailService emailService,
                                UserDetailsServiceImpl userDetailsService, RememberMeServices rememberMeServices,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.userDetailsService = userDetailsService;
        this.rememberMeServices = rememberMeServices;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------- Verification (emailed code or backup code) ----------

    @GetMapping("/verify-2fa")
    public String showVerifyForm(HttpSession session, Model model) {
        if (session.getAttribute("PENDING_2FA_USER") == null) {
            return "redirect:/login";
        }
        model.addAttribute("emailFailed", session.getAttribute("EMAIL_FAILED") != null);
        return "verify-2fa";
    }

    /** One form for both: the emailed 6-digit code, or one of the 8-character backup codes. */
    @PostMapping("/verify-2fa")
    public String verifyCode(@RequestParam String code,
                              HttpServletRequest request,
                              HttpServletResponse response,
                              HttpSession session,
                              Model model) {

        String pendingUsername = (String) session.getAttribute("PENDING_2FA_USER");
        if (pendingUsername == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(pendingUsername).orElseThrow();
        String entered = code == null ? "" : code.trim();

        boolean emailCodeValid = user.getTwoFaCode() != null
                && user.getTwoFaCode().equals(entered)
                && user.getTwoFaCodeExpiry() != null
                && user.getTwoFaCodeExpiry().isAfter(LocalDateTime.now());

        if (emailCodeValid) {
            user.setTwoFaCode(null);
            user.setTwoFaCodeExpiry(null);
            userRepository.save(user);
            return completeLogin(user, request, response, session);
        }

        // Not the emailed code - maybe a backup code (single-use, removed once used)
        if (consumeBackupCode(user, entered)) {
            userRepository.save(user);
            return completeLogin(user, request, response, session);
        }

        model.addAttribute("error", "That code is incorrect, has expired or was already used.");
        return "verify-2fa";
    }

    private boolean consumeBackupCode(User user, String entered) {
        String normalized = entered.replace("-", "").replace(" ", "").toUpperCase();
        if (normalized.length() < 6) {
            return false;
        }
        Iterator<String> it = user.getBackupCodeHashes().iterator();
        while (it.hasNext()) {
            if (passwordEncoder.matches(normalized, it.next())) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    @PostMapping("/resend-2fa-code")
    public String resendCode(HttpSession session, RedirectAttributes redirectAttributes) {
        String pendingUsername = (String) session.getAttribute("PENDING_2FA_USER");
        if (pendingUsername == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(pendingUsername).orElseThrow();

        String code = String.valueOf(100000 + random.nextInt(900000));
        user.setTwoFaCode(code);
        user.setTwoFaCodeExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendTwoFactorCode(user.getEffectiveTwoFaEmail(), code);

        redirectAttributes.addFlashAttribute("resent", true);
        return "redirect:/verify-2fa";
    }

    /** The old separate backup-code page: everything now happens on /verify-2fa. */
    @GetMapping("/verify-backup-code")
    public String oldBackupCodePage() {
        return "redirect:/verify-2fa";
    }

    // ---------- Shared completion logic ----------

    /** Finalizes the login once whichever verification method used has succeeded. */
    private String completeLogin(User user, HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        Boolean rememberMeRequested = (Boolean) session.getAttribute("REMEMBER_ME_REQUESTED");
        if (Boolean.TRUE.equals(rememberMeRequested)) {
            rememberMeServices.loginSuccess(request, response, authToken);
        }

        session.removeAttribute("PENDING_2FA_USER");
        session.removeAttribute("EMAIL_FAILED");
        session.removeAttribute("REMEMBER_ME_REQUESTED");

        return "redirect:" + user.getDashboardPath();
    }
}
