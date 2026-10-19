package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.Broker;
import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.BrokerRepository;
import com.example.ceylonestate.service.BrokerMessageService;
import com.example.ceylonestate.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Messaging between users and brokers.
 *   POST /messages/send                 - send a message / share a listing (any signed-in user)
 *   GET  /broker/inbox                  - the inbox (only for an account linked to a broker)
 *   POST /broker/inbox/{id}/read        - mark one message read
 *   POST /broker/inbox/{id}/unread      - mark one message unread
 *   POST /broker/inbox/{id}/delete      - delete one message
 */
@Controller
public class BrokerMessageController {

    private final BrokerMessageService messageService;
    private final BrokerRepository brokerRepository;
    private final CurrentUserService currentUserService;

    public BrokerMessageController(BrokerMessageService messageService,
                                   BrokerRepository brokerRepository,
                                   CurrentUserService currentUserService) {
        this.messageService = messageService;
        this.brokerRepository = brokerRepository;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/messages/send")
    public String send(@RequestParam Long brokerId,
                       @RequestParam(required = false) Long propertyId,
                       @RequestParam(required = false) String body,
                       @RequestParam(required = false, defaultValue = "broker") String from,
                       RedirectAttributes redirectAttributes) {
        User sender = currentUserService.get().orElseThrow();

        // Go back to the page the person came from (listing page when sharing a listing).
        String back = ("property".equals(from) && propertyId != null)
                ? "/properties/" + propertyId
                : "/brokers/" + brokerId;
        try {
            messageService.send(sender, brokerId, propertyId, body);
            redirectAttributes.addFlashAttribute("flash", "brokerMsg.sent");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:" + back;
    }

    @GetMapping("/broker/inbox")
    public String inbox(Model model) {
        Broker broker = myBroker();
        model.addAttribute("inboxMessages", messageService.inbox(broker));
        return "broker-inbox";
    }

    @PostMapping("/broker/inbox/{id}/read")
    public String markRead(@PathVariable Long id) {
        messageService.setRead(myBroker(), id, true);
        return "redirect:/broker/inbox";
    }

    @PostMapping("/broker/inbox/{id}/unread")
    public String markUnread(@PathVariable Long id) {
        messageService.setRead(myBroker(), id, false);
        return "redirect:/broker/inbox";
    }

    @PostMapping("/broker/inbox/{id}/delete")
    public String delete(@PathVariable Long id) {
        messageService.delete(myBroker(), id);
        return "redirect:/broker/inbox";
    }

    /** The broker profile linked to the signed-in account; anyone else gets a 404. */
    private Broker myBroker() {
        return currentUserService.get()
                .flatMap(brokerRepository::findByAccount)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
