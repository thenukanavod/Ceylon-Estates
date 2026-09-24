package com.example.ceylonestate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import com.example.ceylonestate.service.UserDetailsServiceImpl;
import com.example.ceylonestate.service.CustomOAuth2UserService;
import com.example.ceylonestate.config.TwoFactorAuthenticationSuccessHandler;

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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, TwoFactorAuthenticationSuccessHandler successHandler,
                                            RememberMeServices rememberMeServices,
                                            CustomOAuth2UserService customOAuth2UserService,
                                            OAuth2LoginSuccessHandler oauth2LoginSuccessHandler) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public pages - no login required
                .requestMatchers(
                        "/", "/register", "/login",
                        "/forgot-password", "/reset-password",
                        "/verify-2fa", "/resend-2fa-code",
                        "/verify-backup-code",
                        "/settings",
                        "/oauth2/**", "/login/oauth2/**", "/sso-handoff",
                        "/css/**", "/js/**", "/images/**", "/api/health", "/h2-console/**"
                ).permitAll()
                // Admin-only area - polymorphism decides who lands here, this just enforces it
                .requestMatchers("/admin/**", "/admin").hasRole("ADMIN")
                // Everything else requires login
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
