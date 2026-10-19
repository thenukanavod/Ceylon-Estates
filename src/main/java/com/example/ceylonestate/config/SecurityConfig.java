package com.example.ceylonestate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import com.example.ceylonestate.service.UserDetailsServiceImpl;
import com.example.ceylonestate.service.CustomOAuth2UserService;

/**
 * Two security "doors":
 *   1. The ADMIN portal  - everything under /admin/**, with its own login page (/admin/login).
 *      Only admin accounts can sign in there, there is no "remember me" and no Google sign-in.
 *   2. The public site   - everything else, with the normal /login page.
 *      Admin accounts cannot sign in through it.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Hashes passwords before storing them, and checks hashes on login.
     * Never store plain-text passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RememberMeServices rememberMeServices(UserDetailsServiceImpl userDetailsService) {
        TokenBasedRememberMeServices services = new TokenBasedRememberMeServices("project28-remember-key", userDetailsService);
        services.setTokenValiditySeconds(14 * 24 * 60 * 60); // 14 days
        services.setParameter("remember-me");
        return services;
    }

    // ------------------------------------------------------------------
    // Door 1: the admin portal
    // ------------------------------------------------------------------

    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http,
                                                AdminLoginSuccessHandler adminSuccessHandler) throws Exception {
        http
            .securityMatcher("/admin", "/admin/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/login").permitAll()
                // Polymorphism decides who is an admin; this just enforces it
                .anyRequest().hasRole("ADMIN")
            )
            .formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .successHandler(adminSuccessHandler)
                .failureHandler((request, response, exception) -> response.sendRedirect(
                        request.getContextPath() + (exception instanceof LockedException
                                ? "/admin/login?locked" : "/admin/login?error")))
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?logout")
                .permitAll()
            );
        return http.build();
    }

    // ------------------------------------------------------------------
    // Door 2: the public website
    // ------------------------------------------------------------------

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http, PublicLoginSuccessHandler successHandler,
                                            RememberMeServices rememberMeServices,
                                            CustomOAuth2UserService customOAuth2UserService,
                                            OAuth2LoginSuccessHandler oauth2LoginSuccessHandler) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // A single broker's page is public (but /brokers/apply and the POST actions still need login)
                .requestMatchers(HttpMethod.GET, "/brokers/{id:[0-9]+}").permitAll()
                // Public pages - no login required
                .requestMatchers(
                        "/", "/register", "/login",
                        "/forgot-password", "/reset-password",
                        "/verify-2fa", "/resend-2fa-code",
                        "/verify-backup-code",
                        "/settings",
                        "/oauth2/**", "/login/oauth2/**", "/sso-handoff",
                        "/css/**", "/js/**", "/images/**", "/api/health", "/h2-console/**",
                        // Public property pages - anyone can browse without an account
                        "/properties", "/properties/**", "/projects",
                        "/lands", "/houses", "/apartments", "/about",
                        "/brokers", "/articles", "/articles/**", "/local-info", "/local-info/suggest", "/contact",
                        // Uploaded listing photos must be visible to visitors, and the error page must render
                        "/uploads/**", "/error"
                ).permitAll()
                // Everything else requires login (the admin area is handled by the admin chain above)
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                // Success doesn't mean "logged in" yet - see TwoFactorAuthenticationSuccessHandler
                .successHandler(successHandler)
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(oauth2LoginSuccessHandler)
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .rememberMeServices(rememberMeServices)
            )
            // Needed so the H2 console (if used) can render in a frame
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            // Disabled for simplicity during development; consider enabling for production
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));

        return http.build();
    }
}
