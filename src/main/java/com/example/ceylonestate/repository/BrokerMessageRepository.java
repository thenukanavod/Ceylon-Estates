package com.example.ceylonestate.repository;

import com.example.ceylonestate.model.Broker;
import com.example.ceylonestate.model.BrokerMessage;
import com.example.ceylonestate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BrokerMessageRepository extends JpaRepository<BrokerMessage, Long> {

    /** A broker's inbox, newest first. Sender and shared listing are loaded together. */
    @Query("select m from BrokerMessage m join fetch m.sender left join fetch m.property "
            + "where m.broker = ?1 order by m.createdAt desc")
    List<BrokerMessage> inboxFor(Broker broker);

    long countByBrokerAndReadAtIsNull(Broker broker);

    Optional<BrokerMessage> findByIdAndBroker(Long id, Broker broker);

    /** Used to stop one account flooding brokers with messages. */
    long countBySenderAndCreatedAtAfter(User sender, LocalDateTime since);
}
