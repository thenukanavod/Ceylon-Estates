package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.ContactMessage;
import com.example.ceylonestate.model.MessageStatus;
import com.example.ceylonestate.repository.ContactMessageRepository;
import com.example.ceylonestate.service.CurrentUserService;
import com.example.ceylonestate.service.EmailService;
import com.example.ceylonestate.service.MessageCsvService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The admin inbox for messages sent through the public Contact us form:
 * read them, reply by email, archive them or delete them.
 */
@Controller
@RequestMapping("/admin/messages")
public class AdminMessageController {

    private final ContactMessageRepository repository;
    private final EmailService emailService;
    private final CurrentUserService currentUserService;
    private final MessageCsvService csvService;

    public AdminMessageController(ContactMessageRepository repository,
                                  EmailService emailService,
                                  CurrentUserService currentUserService,
                                  MessageCsvService csvService) {
        this.csvService = csvService;
        this.repository = repository;
        this.emailService = emailService;
        this.currentUserService = currentUserService;
    }

    /** Inbox list. {@code filter} is all (default), new, read or archived. */
    @GetMapping
    public String inbox(@RequestParam(defaultValue = "all") String filter, Model model) {
        List<ContactMessage> messages;
        switch (filter) {
            case "new" -> messages = repository.findByStatusOrderByCreatedAtDesc(MessageStatus.NEW);
            case "read" -> messages = repository.findByStatusOrderByCreatedAtDesc(MessageStatus.READ);
            case "archived" -> messages = repository.findByStatusOrderByCreatedAtDesc(MessageStatus.ARCHIVED);
            default -> {
                filter = "all";
                messages = repository.findAllByOrderByCreatedAtDesc();
            }
        }
        model.addAttribute("messages", messages);
        model.addAttribute("filter", filter);
        model.addAttribute("countAll", repository.count());
        model.addAttribute("countNew", repository.countByStatus(MessageStatus.NEW));
        model.addAttribute("countRead", repository.countByStatus(MessageStatus.READ));
        model.addAttribute("countArchived", repository.countByStatus(MessageStatus.ARCHIVED));
        return "admin/messages";
    }

    /** Download the messages (same filter as the inbox) as a CSV file. */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "all") String filter) {
        List<ContactMessage> messages = switch (filter) {
            case "new" -> repository.findByStatusOrderByCreatedAtDesc(MessageStatus.NEW);
            case "read" -> repository.findByStatusOrderByCreatedAtDesc(MessageStatus.READ);
            case "archived" -> repository.findByStatusOrderByCreatedAtDesc(MessageStatus.ARCHIVED);
            default -> repository.findAllByOrderByCreatedAtDesc();
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=messages.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvService.toCsv(messages).getBytes(StandardCharsets.UTF_8));
    }

    /** Opening a message marks it as read. */
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        ContactMessage message = find(id);
        if (message.getStatus() == MessageStatus.NEW) {
            message.setStatus(MessageStatus.READ);
            repository.save(message);
        }
        model.addAttribute("message", message);
        return "admin/message-detail";
    }

    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Long id,
                        @RequestParam String reply,
                        RedirectAttributes redirectAttributes) {
        ContactMessage message = find(id);
        String text = reply == null ? "" : reply.trim();
        if (text.length() < 2 || text.length() > 2000) {
            redirectAttributes.addFlashAttribute("flashError", "adminMessages.err.replyLength");
            return "redirect:/admin/messages/" + id;
        }
        try {
            emailService.sendContactReply(message.getEmail(), message.getSubject(), text);
        } catch (MailException e) {
            redirectAttributes.addFlashAttribute("flashError", "adminMessages.err.mailFailed");
            return "redirect:/admin/messages/" + id;
        }
        String admin = currentUserService.get().map(u -> u.getUsername()).orElse("admin");
        message.markReplied(text, admin);
        repository.save(message);
        redirectAttributes.addFlashAttribute("flash", "adminMessages.flash.replied");
        return "redirect:/admin/messages/" + id;
    }

    /** Move a message between New, Read and Archived. */
    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam String value,
                               RedirectAttributes redirectAttributes) {
        ContactMessage message = find(id);
        try {
            message.setStatus(MessageStatus.valueOf(value.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        repository.save(message);
        redirectAttributes.addFlashAttribute("flash", "adminMessages.flash.updated");
        return "redirect:/admin/messages";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        repository.delete(find(id));
        redirectAttributes.addFlashAttribute("flash", "adminMessages.flash.deleted");
        return "redirect:/admin/messages";
    }

    private ContactMessage find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
