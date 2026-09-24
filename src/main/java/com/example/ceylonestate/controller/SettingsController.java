package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class SettingsController {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no ambiguous chars (I, O, 0, 1)
    private static final int BACKUP_CODE_COUNT = 8;
    private static final int BACKUP_CODE_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public SettingsController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/settings")
    public String showSettings() {
        return "settings";
    }

    /** Turns 2FA on/off and sets the chosen method. */
    @PostMapping("/settings/2fa")
    public String updateTwoFa(Authentication authentication,
                               @RequestParam(required = false) String twoFaEnabled,
                               @RequestParam(defaultValue = "EMAIL") String twoFaMethod,
                               @RequestParam(required = false) String twoFaEmail,
                               RedirectAttributes redirectAttributes) {

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();

        boolean enabled = "on".equals(twoFaEnabled);
        user.setTwoFaEnabled(enabled);
        user.setTwoFaMethod(twoFaMethod);
        user.setTwoFaEmail(twoFaEmail);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("twoFaUpdated", true);
        return "redirect:/settings";
    }

    /** Generates a fresh set of backup codes, replacing any existing ones, and shows them ONCE. */
    @PostMapping("/settings/2fa/backup-codes")
    public String generateBackupCodes(Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();

        List<String> plainCodes = IntStream.range(0, BACKUP_CODE_COUNT)
                .mapToObj(i -> generateOneCode())
                .collect(Collectors.toList());

        Set<String> hashes = plainCodes.stream()
                .map(passwordEncoder::encode)
                .collect(Collectors.toCollection(HashSet::new));

        user.setBackupCodeHashes(hashes);
        userRepository.save(user);

        // Shown once via flash attribute - we don't store the plain codes anywhere
        redirectAttributes.addFlashAttribute("newBackupCodes", plainCodes);
        return "redirect:/settings";
    }

    private String generateOneCode() {
        StringBuilder sb = new StringBuilder(BACKUP_CODE_LENGTH);
        for (int i = 0; i < BACKUP_CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
