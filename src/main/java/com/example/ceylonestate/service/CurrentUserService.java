package com.example.ceylonestate.service;

import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Answers "who is logged in right now?" for the controllers, using the same
 * lookup the navbar's GlobalModelAttributes uses. Works for password logins
 * and Google logins alike, because both end up with the app's own
 * username-based authentication.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** The logged-in user, or empty for visitors who are not signed in. */
    public Optional<User> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return userRepository.findByUsername(auth.getName());
    }

    public boolean isAdmin() {
        return get().map(user -> "ADMIN".equals(user.getRole())).orElse(false);
    }
}
