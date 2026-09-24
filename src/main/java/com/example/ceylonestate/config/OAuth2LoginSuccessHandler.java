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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Runs after Google confirms who someone is (via CustomOAuth2UserService).
 * Swaps Google's authentication for our own, role-based one, so the rest of
 * the app (hasRole('ADMIN') checks, getDashboardPath(), etc.) works exactly
 * the same regardless of whether someone logged in with a password or Google.
 *
 * 2FA is intentionally skipped here - Google already handles strong
 * authentication (including their own 2-Step Verification) on their end.
 *
 * Since this whole flow necessarily happens on localhost (Google won't
 * accept our custom local domain as a redirect URI), we finish by handing
 * the login off to the branded domain via a short-lived token, rather than
 * leaving the user stranded on localhost for the rest of their session.
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

        // Hand off to the branded domain with a short-lived, single-use token
        String token = ssoHandoffService.createToken(user.getUsername());
        String redirectPath = URLEncoder.encode(user.getDashboardPath(), StandardCharsets.UTF_8);
        response.sendRedirect("http://ceylonestates.local/sso-handoff?token=" + token + "&redirect=" + redirectPath);
    }
}
