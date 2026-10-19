package com.example.ceylonestate.config;

import com.example.ceylonestate.model.Broker;
import com.example.ceylonestate.repository.BrokerMessageRepository;
import com.example.ceylonestate.repository.BrokerRepository;
import com.example.ceylonestate.service.CurrentUserService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Makes two values available to every page: "myBroker" (the broker profile linked
 * to the signed-in account, or null) and "unreadBrokerMessages". The navigation
 * uses them to show the Broker inbox link only to brokers.
 */
@ControllerAdvice
public class BrokerModelAttributes {

    private final CurrentUserService currentUserService;
    private final BrokerRepository brokerRepository;
    private final BrokerMessageRepository messageRepository;

    public BrokerModelAttributes(CurrentUserService currentUserService,
                                 BrokerRepository brokerRepository,
                                 BrokerMessageRepository messageRepository) {
        this.currentUserService = currentUserService;
        this.brokerRepository = brokerRepository;
        this.messageRepository = messageRepository;
    }

    @ModelAttribute("myBroker")
    public Broker myBroker() {
        return currentUserService.get().flatMap(brokerRepository::findByAccount).orElse(null);
    }

    @ModelAttribute("unreadBrokerMessages")
    public long unreadBrokerMessages() {
        Broker broker = myBroker();
        return broker == null ? 0 : messageRepository.countByBrokerAndReadAtIsNull(broker);
    }
}
