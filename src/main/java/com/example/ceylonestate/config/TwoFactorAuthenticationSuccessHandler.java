package com.example.ceylonestate.config;

import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.UserRepository;
import com.example.ceylonestate.service.EmailService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Runs immediately after Spring Security confirms the username/password are correct.
 *
 * If the user has 2FA turned OFF (default for new accounts), they're let straight in -
 * Spring Security's own login flow (including remember-me) already handled everything,
 * so we just redirect them to the right dashboard.
 *
 * If 2FA is ON, this wipes the session Spring Security just created (so they're NOT
 * actually logged in yet) and sends them to verify via whichever method they chose:
 * an emailed code, or a saved backup code.
 */
@Component
public class TwoFactorAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public TwoFactorAuthenticationSuccessHandler(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        if (!user.isTwoFaEnabled()) {
            // 2FA is off - the login Spring Security already granted stands as-is.
            response.sendRedirect(user.getDashboardPath());
            return;
        }

        // Remember whether "Remember me" was checked - we need this AFTER verification
        // succeeds, but we're about to wipe this session, so save it into the new one.
        boolean rememberMeRequested = request.getParameter("remember-me") != null;

        // Undo the full login Spring Security just granted - they only get real
        // access after TwoFactorController confirms their code.
        SecurityContextHolder.clearContext();
        request.getSession().invalidate();

        HttpSession newSession = request.getSession(true);
        newSession.setAttribute("PENDING_2FA_USER", username);
        newSession.setAttribute("REMEMBER_ME_REQUESTED", rememberMeRequested);

        // Expire any remember-me cookie so it can't silently skip verification next visit
        Cookie rememberMeCookie = new Cookie("remember-me", null);
        rememberMeCookie.setPath("/");
        rememberMeCookie.setMaxAge(0);
        response.addCookie(rememberMeCookie);

        if ("BACKUP_CODES".equals(user.getTwoFaMethod())) {
            response.sendRedirect("/verify-backup-code");
            return;
        }

        // Default: email a code
        String code = String.valueOf(100000 + random.nextInt(900000)); // 6 digits
        user.setTwoFaCode(code);
        user.setTwoFaCodeExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);
        emailService.sendTwoFactorCode(user.getEffectiveTwoFaEmail(), code);

        response.sendRedirect("/verify-2fa");
    }
}
