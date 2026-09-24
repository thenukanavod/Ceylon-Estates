package com.example.ceylonestate.controller;

import com.example.ceylonestate.service.SsoHandoffService;
import com.example.ceylonestate.service.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Receives the handoff from Google login (which necessarily happened on
 * localhost) and establishes a genuine login session here on the branded
 * domain, using a short-lived, single-use token instead of trusting the
 * request blindly.
 */
@Controller
public class SsoHandoffController {

    private final SsoHandoffService ssoHandoffService;
    private final UserDetailsServiceImpl userDetailsService;

    public SsoHandoffController(SsoHandoffService ssoHandoffService, UserDetailsServiceImpl userDetailsService) {
        this.ssoHandoffService = ssoHandoffService;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/sso-handoff")
    public String handoff(@RequestParam String token,
                           @RequestParam(defaultValue = "/") String redirect,
                           HttpServletRequest request) {

        String username = ssoHandoffService.consumeToken(token);
        if (username == null) {
            // Expired, already used, or forged - just send them to log in normally
            return "redirect:/login";
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return "redirect:" + redirect;
    }
}
