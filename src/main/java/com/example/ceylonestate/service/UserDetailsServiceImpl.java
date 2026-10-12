package com.example.ceylonestate.service;

import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Tells Spring Security how to look up a user by username when someone
 * tries to log in, and which role/authority to grant them.
 *
 * OOP in action: we call user.getRole() here without caring whether "user"
 * is actually a RegularUser or AdminUser underneath - polymorphism means
 * the correct role comes back automatically either way.
 *
 * An account that failed too many logins is reported as locked, which makes
 * Spring Security refuse the login for a while (see LoginAttemptService).
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;

    public UserDetailsServiceImpl(UserRepository userRepository, LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user found: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword()) // already hashed
                .roles(user.getRole()) // polymorphic - "USER" or "ADMIN" depending on subclass
                .accountLocked(loginAttemptService.isLocked(user.getUsername()))
                .build();
    }
}
