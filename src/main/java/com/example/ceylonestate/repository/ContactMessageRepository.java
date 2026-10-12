package com.example.ceylonestate.repository;

import com.example.ceylonestate.model.ContactMessage;
import com.example.ceylonestate.model.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    List<ContactMessage> findAllByOrderByCreatedAtDesc();

    List<ContactMessage> findByStatusOrderByCreatedAtDesc(MessageStatus status);

    List<ContactMessage> findTop5ByStatusOrderByCreatedAtDesc(MessageStatus status);

    long countByStatus(MessageStatus status);
}
