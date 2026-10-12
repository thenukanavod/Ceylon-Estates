package com.example.ceylonestate.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base class for all account types.
 *
 * OOP: Encapsulation - all fields are private, accessed only via getters/setters.
 * OOP: Inheritance - AdminUser and RegularUser extend this class and add their
 *      own specific behaviour.
 * OOP: Polymorphism - getRole() and getDashboardPath() are overridden differently
 *      by each subclass, so calling them on a User reference gives different
 *      results depending on the actual runtime type.
 *
 * Stored as a single database table (SINGLE_TABLE inheritance) with a
 * discriminator column "user_type" that records which subclass each row is.
 */
@Entity
@Table(name = "app_user")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type")
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password; // stored as a BCrypt hash, never plain text

    @Column
    private String backupEmail; // optional

    @Column
    private String phoneNumber; // optional

    @Column
    private String gender; // "MALE" or "FEMALE", optional

    // --- OAuth (e.g. "Sign in with Google") ---
    @Column
    private String oauthProvider; // "GOOGLE", or null for normal accounts

    // --- Password reset support ---
    @Column
    private String resetToken;

    @Column
    private LocalDateTime resetTokenExpiry;

    // --- Two-factor authentication support ---
    @Column(nullable = false)
    private boolean twoFaEnabled = false; // off by default - user opts in via Settings

    @Column
    private String twoFaMethod = "EMAIL"; // "EMAIL" or "BACKUP_CODES"

    @Column
    private String twoFaEmail; // optional - if set, 2FA codes go here instead of the main email

    @Column
    private String twoFaCode;

    @Column
    private LocalDateTime twoFaCodeExpiry;

    @ElementCollection
    @CollectionTable(name = "user_backup_codes", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "code_hash")
    private Set<String> backupCodeHashes = new HashSet<>();

    // --- Profile picture ---
    @Column
    private String avatarFilename; // just the filename - actual file lives in /uploads/avatars/

    protected User() {
        // required by JPA
    }

    protected User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    /**
     * Polymorphic method - each subclass returns its own role name.
     * Used to grant the correct Spring Security authority at login.
     */
    public abstract String getRole();

    /**
     * Polymorphic method - each subclass decides where it lands after login.
     */
    public abstract String getDashboardPath();

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBackupEmail() {
        return backupEmail;
    }

    public void setBackupEmail(String backupEmail) {
        this.backupEmail = backupEmail;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getOauthProvider() {
        return oauthProvider;
    }

    public void setOauthProvider(String oauthProvider) {
        this.oauthProvider = oauthProvider;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public LocalDateTime getResetTokenExpiry() {
        return resetTokenExpiry;
    }

    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) {
        this.resetTokenExpiry = resetTokenExpiry;
    }

    public String getTwoFaCode() {
        return twoFaCode;
    }

    public void setTwoFaCode(String twoFaCode) {
        this.twoFaCode = twoFaCode;
    }

    public LocalDateTime getTwoFaCodeExpiry() {
        return twoFaCodeExpiry;
    }

    public void setTwoFaCodeExpiry(LocalDateTime twoFaCodeExpiry) {
        this.twoFaCodeExpiry = twoFaCodeExpiry;
    }

    public boolean isTwoFaEnabled() {
        return twoFaEnabled;
    }

    public void setTwoFaEnabled(boolean twoFaEnabled) {
        this.twoFaEnabled = twoFaEnabled;
    }

    public String getTwoFaMethod() {
        return twoFaMethod;
    }

    public void setTwoFaMethod(String twoFaMethod) {
        this.twoFaMethod = twoFaMethod;
    }

    public String getTwoFaEmail() {
        return twoFaEmail;
    }

    public void setTwoFaEmail(String twoFaEmail) {
        this.twoFaEmail = twoFaEmail;
    }

    /** The address 2FA codes should actually be sent to - falls back to the main email if none set. */
    public String getEffectiveTwoFaEmail() {
        return (twoFaEmail != null && !twoFaEmail.isBlank()) ? twoFaEmail : getEmail();
    }

    public Set<String> getBackupCodeHashes() {
        return backupCodeHashes;
    }

    public void setBackupCodeHashes(Set<String> backupCodeHashes) {
        this.backupCodeHashes = backupCodeHashes;
    }

    public String getAvatarFilename() {
        return avatarFilename;
    }

    public void setAvatarFilename(String avatarFilename) {
        this.avatarFilename = avatarFilename;
    }
}
