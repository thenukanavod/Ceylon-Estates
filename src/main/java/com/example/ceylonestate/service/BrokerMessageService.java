package com.example.ceylonestate.service;

import com.example.ceylonestate.model.Broker;
import com.example.ceylonestate.model.BrokerMessage;
import com.example.ceylonestate.model.Property;
import com.example.ceylonestate.model.User;
import com.example.ceylonestate.repository.BrokerMessageRepository;
import com.example.ceylonestate.repository.BrokerRepository;
import com.example.ceylonestate.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sending a message (or a shared listing) to a broker, and the broker's inbox.
 * Errors are thrown as IllegalArgumentException with a readable message, the
 * same way BrokerReviewService does it.
 */
@Service
public class BrokerMessageService {

    /** One account may send at most this many messages per hour. */
    private static final int MAX_PER_HOUR = 20;

    private final BrokerMessageRepository messageRepository;
    private final BrokerRepository brokerRepository;
    private final PropertyRepository propertyRepository;

    public BrokerMessageService(BrokerMessageRepository messageRepository,
                                BrokerRepository brokerRepository,
                                PropertyRepository propertyRepository) {
        this.messageRepository = messageRepository;
        this.brokerRepository = brokerRepository;
        this.propertyRepository = propertyRepository;
    }

    /** Sends a message, a shared listing, or both. */
    @Transactional
    public void send(User sender, Long brokerId, Long propertyId, String body) {
        Broker broker = brokerRepository.findById(brokerId)
                .orElseThrow(() -> new IllegalArgumentException("That broker no longer exists."));

        if (broker.getAccount() != null && broker.getAccount().getId().equals(sender.getId())) {
            throw new IllegalArgumentException("You cannot send a message to yourself.");
        }

        Property property = null;
        if (propertyId != null) {
            property = propertyRepository.findById(propertyId)
                    .orElseThrow(() -> new IllegalArgumentException("That listing no longer exists."));
            // Only live listings (or the sender's own) can be shared.
            if (!property.getStatus().isLive() && !property.isOwnedBy(sender)) {
                throw new IllegalArgumentException("That listing cannot be shared.");
            }
        }

        String text = body == null ? null : body.trim();
        if (text != null && text.isEmpty()) {
            text = null;
        }
        if (text == null && property == null) {
            throw new IllegalArgumentException("Please write a message.");
        }
        if (text != null && text.length() > BrokerMessage.MAX_BODY_LENGTH) {
            throw new IllegalArgumentException("Messages can be at most " + BrokerMessage.MAX_BODY_LENGTH + " characters.");
        }

        long recent = messageRepository.countBySenderAndCreatedAtAfter(sender, LocalDateTime.now().minusHours(1));
        if (recent >= MAX_PER_HOUR) {
            throw new IllegalArgumentException("You are sending messages too quickly. Please try again later.");
        }

        messageRepository.save(new BrokerMessage(broker, sender, property, text));
    }

    @Transactional(readOnly = true)
    public List<BrokerMessage> inbox(Broker broker) {
        return messageRepository.inboxFor(broker);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Broker broker) {
        return messageRepository.countByBrokerAndReadAtIsNull(broker);
    }

    /** Only the inbox owner can change a message; anything else is silently ignored. */
    @Transactional
    public void setRead(Broker broker, Long messageId, boolean read) {
        messageRepository.findByIdAndBroker(messageId, broker).ifPresent(m -> {
            if (read) {
                m.markRead();
            } else {
                m.markUnread();
            }
            messageRepository.save(m);
        });
    }

    @Transactional
    public void delete(Broker broker, Long messageId) {
        messageRepository.findByIdAndBroker(messageId, broker).ifPresent(messageRepository::delete);
    }
}
