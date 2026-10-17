package com.example.ceylonestate.config;

import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.UserRepository;
import com.example.ceylonestate.service.SsoHandoffService;
import com.example.ceylonestate.service.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Runs after Google confirms who someone is (via CustomOAuth2UserService).
 * Swaps Google's authentication for our own, role-based one, so the rest of
 * the app (hasRole('ADMIN') checks, getDashboardPath(), etc.) works exactly
 * the same regardless of whether someone logged in with a password or Google.
 *
 * 2FA is intentionally skipped here - Google already handles strong
 * authentication (including their own 2-Step Verification) on their end.
 *
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final SsoHandoffService ssoHandoffService;

    public OAuth2LoginSuccessHandler(UserRepository userRepository, UserDetailsServiceImpl userDetailsService,
                                      SsoHandoffService ssoHandoffService) {
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.ssoHandoffService = ssoHandoffService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");

        User user = userRepository.findByEmail(email).orElseThrow();

        // Admin accounts may only sign in through the admin portal (/admin/login) with a password.
        // Without this check, a Google account with an admin's email would skip that door (and 2FA).
        if ("ADMIN".equals(user.getRole())) {
            SecurityContextHolder.clearContext();
            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/login?error");
            return;
        }

        // Establish the session here on localhost too, so things work even if
        // someone stays on localhost for some reason.
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        // The site is served at http://localhost:8080/ceylonestates, the same address Google
        // sent the user back to, so the session created above is already the right one.
        response.sendRedirect(request.getContextPath() + user.getDashboardPath());
    }
}
