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
 * Success handler for /admin/login. Only ADMIN accounts may get in through the
 * admin portal - a normal user with a correct password is signed straight back out.
 * Admins then go through the same 2FA step as everybody else (if they turned it on).
 */
@Component
public class AdminLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final TwoFactorAuthenticationSuccessHandler twoFactorHandler;

    public AdminLoginSuccessHandler(UserRepository userRepository,
                                    TwoFactorAuthenticationSuccessHandler twoFactorHandler) {
        this.userRepository = userRepository;
        this.twoFactorHandler = twoFactorHandler;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/admin/login?error");
            return;
        }
        twoFactorHandler.onAuthenticationSuccess(request, response, authentication);
    }
}
