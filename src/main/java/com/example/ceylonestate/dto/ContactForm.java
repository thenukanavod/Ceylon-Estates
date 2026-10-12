package com.example.ceylonestate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Form-backing object for the public Contact us page. */
public class ContactForm {

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Email
    @Size(max = 160)
    private String email;

    @Size(max = 30)
    private String phone;

    @NotBlank
    @Size(max = 150)
    private String subject;

    @NotBlank
    @Size(min = 10, max = 2000)
    private String message;

    /** Honeypot: hidden from people, filled in by spam bots. Must stay empty. */
    private String website;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
}
