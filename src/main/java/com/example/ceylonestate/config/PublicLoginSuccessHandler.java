package com.example.ceylonestate.config;

import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Success handler for the normal /login page. Administrators must use the
 * separate admin portal, so an admin account that signs in here is turned away
 * with the same message as a wrong password (we don't reveal which accounts are admins).
 * Everyone else continues to the existing 2FA / dashboard logic.
 */
@Component
public class PublicLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final TwoFactorAuthenticationSuccessHandler twoFactorHandler;

    public PublicLoginSuccessHandler(UserRepository userRepository,
                                     TwoFactorAuthenticationSuccessHandler twoFactorHandler) {
        this.userRepository = userRepository;
        this.twoFactorHandler = twoFactorHandler;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user != null && "ADMIN".equals(user.getRole())) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/login?error");
            return;
        }
        twoFactorHandler.onAuthenticationSuccess(request, response, authentication);
    }
}
