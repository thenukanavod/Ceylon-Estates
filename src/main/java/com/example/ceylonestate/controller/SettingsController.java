package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.UserRepository;
import com.example.ceylonestate.service.CurrentUserService;
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

/**
 * App settings. Two-factor authentication is ONE feature with one on/off switch:
 * when it is on, a login needs the emailed code, and the backup codes work as a
 * fallback on that same verification page if the email does not arrive.
 *   POST /settings/2fa               - switch 2FA on or off
 *   POST /settings/2fa/email         - choose where the code is emailed
 *   POST /settings/2fa/backup-codes  - make a fresh set of backup codes
 */
@Controller
public class SettingsController {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no ambiguous chars (I, O, 0, 1)
    private static final int BACKUP_CODE_COUNT = 8;
    private static final int BACKUP_CODE_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final SecureRandom random = new SecureRandom();

    public SettingsController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                              CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/settings")
    public String showSettings(Model model) {
        // The page is public (theme and language), so there may be nobody signed in.
        currentUserService.get().ifPresent(u ->
                model.addAttribute("backupCodesLeft", u.getBackupCodeHashes().size()));
        model.addAttribute("backupCodesTotal", BACKUP_CODE_COUNT);
        return "settings";
    }

    /** The big on/off switch. Turning it on also makes the first set of backup codes. */
    @PostMapping("/settings/2fa")
    public String updateTwoFa(@RequestParam(required = false) String twoFaEnabled,
                               RedirectAttributes redirectAttributes) {

        User user = currentUserService.get().orElseThrow();
        boolean enabled = "on".equals(twoFaEnabled);

        user.setTwoFaEnabled(enabled);
        user.setTwoFaMethod("EMAIL"); // backup codes are a fallback, not a separate method
        user.setTwoFaCode(null);
        user.setTwoFaCodeExpiry(null);

        if (enabled) {
            if (user.getBackupCodeHashes().isEmpty()) {
                redirectAttributes.addFlashAttribute("newBackupCodes", replaceBackupCodes(user));
            }
        } else {
            user.getBackupCodeHashes().clear(); // off means nothing is left over
        }
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("twoFaMessage", enabled ? "settings.twoFaOn" : "settings.twoFaOff");
        return "redirect:/settings";
    }

    /** Where the emailed code is sent (blank = the main account email). */
    @PostMapping("/settings/2fa/email")
    public String updateTwoFaEmail(@RequestParam(required = false) String twoFaEmail,
                                    RedirectAttributes redirectAttributes) {
        User user = currentUserService.get().orElseThrow();
        String email = twoFaEmail == null ? "" : twoFaEmail.trim();
        if (!email.isEmpty() && (email.length() > 120 || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) {
            redirectAttributes.addFlashAttribute("twoFaError", "settings.twoFaEmailInvalid");
            return "redirect:/settings";
        }
        user.setTwoFaEmail(email.isEmpty() ? null : email);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("twoFaMessage", "settings.twoFaUpdated");
        return "redirect:/settings";
    }

    /** Generates a fresh set of backup codes, replacing any existing ones, and shows them ONCE. */
    @PostMapping("/settings/2fa/backup-codes")
    public String generateBackupCodes(RedirectAttributes redirectAttributes) {
        User user = currentUserService.get().orElseThrow();
        if (!user.isTwoFaEnabled()) {
            return "redirect:/settings";
        }
        // Shown once via flash attribute - we don't store the plain codes anywhere
        redirectAttributes.addFlashAttribute("newBackupCodes", replaceBackupCodes(user));
        userRepository.save(user);
        return "redirect:/settings";
    }

    /** Replaces the user's stored (hashed) codes and returns the plain ones to show once. */
    private List<String> replaceBackupCodes(User user) {
        List<String> plainCodes = IntStream.range(0, BACKUP_CODE_COUNT)
                .mapToObj(i -> generateOneCode())
                .collect(Collectors.toList());

        Set<String> hashes = plainCodes.stream()
                .map(passwordEncoder::encode)
                .collect(Collectors.toCollection(HashSet::new));

        user.setBackupCodeHashes(hashes);
        return plainCodes;
    }

    private String generateOneCode() {
        StringBuilder sb = new StringBuilder(BACKUP_CODE_LENGTH);
        for (int i = 0; i < BACKUP_CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
