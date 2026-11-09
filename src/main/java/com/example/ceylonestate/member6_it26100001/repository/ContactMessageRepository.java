package com.example.ceylonestate.member6_it26100001.repository;

import com.example.ceylonestate.member6_it26100001.model.ContactMessage;
import com.example.ceylonestate.member6_it26100001.model.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    List<ContactMessage> findAllByOrderByCreatedAtDesc();

    List<ContactMessage> findByStatusOrderByCreatedAtDesc(MessageStatus status);

    List<ContactMessage> findTop5ByStatusOrderByCreatedAtDesc(MessageStatus status);

    long countByStatus(MessageStatus status);
}
