package com.example.ceylonestate.service;

import com.example.ceylonestate.model.RegularUser;
import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Runs whenever someone signs in with Google. Finds their existing account by
 * email, or creates a brand new RegularUser account automatically if this is
 * their first time - same idea as normal registration, just triggered by
 * Google confirming who they are instead of a password.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomOAuth2UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        if (email == null) {
            throw new OAuth2AuthenticationException("Google did not provide an email address.");
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> createGoogleUser(email, name));

        // Mark it as Google-linked even if the account already existed (e.g. they
        // registered normally first, then later used "Sign in with Google" with the same email)
        if (user.getOauthProvider() == null) {
            user.setOauthProvider("GOOGLE");
            userRepository.save(user);
        }

        return oauth2User;
    }

    private User createGoogleUser(String email, String name) {
        String username = generateUniqueUsername(name != null ? name : email);

        // OAuth accounts never log in with a password, so we generate one they'll
        // never see or need - the account is still valid/consistent in the database.
        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        User user = new RegularUser(username, email, randomPassword);
        user.setOauthProvider("GOOGLE");
        return userRepository.save(user);
    }

    private String generateUniqueUsername(String base) {
        String cleaned = base.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (cleaned.isBlank()) cleaned = "user";

        String candidate = cleaned;
        int suffix = 1;
        while (userRepository.findByUsername(candidate).isPresent()) {
            candidate = cleaned + suffix;
            suffix++;
        }
        return candidate;
    }
}
