package com.example.ceylonestate.service;

import com.example.ceylonestate.model.AdminUser;
import com.example.ceylonestate.model.Property;
import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.PropertyRepository;
import com.example.ceylonestate.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Admin actions on user accounts: promote, demote, delete.
 * Guards: an admin cannot demote or delete themselves, and the last admin can never be removed.
 */
@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final AvatarStorageService avatarStorageService;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(UserRepository userRepository,
                            PropertyRepository propertyRepository,
                            AvatarStorageService avatarStorageService,
                            PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.avatarStorageService = avatarStorageService;
    }

    /**
     * Create: a new admin account made directly by an existing admin.
     * Problems are reported as IllegalArgumentException carrying a message key.
     */
    @Transactional
    public AdminUser createAdmin(String username, String email, String password, String confirmPassword) {
        String name = username == null ? "" : username.trim();
        String mail = email == null ? "" : email.trim();
        if (!name.matches("[A-Za-z0-9_.-]{3,30}")) {
            throw new IllegalArgumentException("adminUsers.new.err.username");
        }
        if (!mail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || mail.length() > 120) {
            throw new IllegalArgumentException("adminUsers.new.err.email");
        }
        if (password == null || password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("adminUsers.new.err.password");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("adminUsers.new.err.mismatch");
        }
        if (userRepository.findByUsername(name).isPresent()) {
            throw new IllegalArgumentException("adminUsers.new.err.usernameTaken");
        }
        if (userRepository.findByEmail(mail).isPresent()) {
            throw new IllegalArgumentException("adminUsers.new.err.emailTaken");
        }
        return userRepository.save(new AdminUser(name, mail, passwordEncoder.encode(password)));
    }

    /** Returns the username of the account that was changed. */
    @Transactional
    public String promote(Long id) {
        User user = find(id);
        if (!"ADMIN".equals(user.getRole())) {
            userRepository.changeType(id, "ADMIN");
        }
        return user.getUsername();
    }

    @Transactional
    public String demote(Long id, String actingUsername) {
        User user = find(id);
        if ("ADMIN".equals(user.getRole())) {
            checkNotSelf(user, actingUsername);
            checkNotLastAdmin(user);
            userRepository.changeType(id, "REGULAR");
        }
        return user.getUsername();
    }

    @Transactional
    public String delete(Long id, String actingUsername) {
        User user = find(id);
        checkNotSelf(user, actingUsername);
        checkNotLastAdmin(user);

        // Keep their listings on the site, just without an owner.
        List<Property> owned = propertyRepository.findByOwnerOrderByIdDesc(user);
        for (Property property : owned) {
            property.setOwner(null);
        }
        propertyRepository.saveAll(owned);

        avatarStorageService.delete(user.getAvatarFilename());
        userRepository.delete(user);
        return user.getUsername();
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NoSuchElementException("No such user"));
    }

    private void checkNotSelf(User user, String actingUsername) {
        if (user.getUsername().equals(actingUsername)) {
            throw new IllegalStateException("self");
        }
    }

    private void checkNotLastAdmin(User user) {
        if ("ADMIN".equals(user.getRole()) && userRepository.countAdmins() <= 1) {
            throw new IllegalStateException("lastAdmin");
        }
    }
}
