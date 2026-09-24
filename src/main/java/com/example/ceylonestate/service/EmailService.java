package com.example.ceylonestate.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Wraps Spring's mail sending so the rest of the app doesn't need to know
 * the details of how (or whether) email actually gets delivered.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toAddress, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toAddress);
        message.setSubject("Reset your password - Ceylon Estates");
        message.setText(
                "We received a request to reset your password.\n\n" +
                "Click the link below to choose a new password:\n" +
                resetLink + "\n\n" +
                "This link expires in 30 minutes. If you didn't request this, you can ignore this email."
        );
        mailSender.send(message);
    }

    public void sendTwoFactorCode(String toAddress, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toAddress);
        message.setSubject("Your login verification code - Ceylon Estates");
        message.setText(
                "Your verification code is: " + code + "\n\n" +
                "Enter this on the login verification page to finish signing in.\n" +
                "This code expires in 10 minutes. If you didn't try to log in, you can ignore this email."
        );
        mailSender.send(message);
    }
}
