package com.example.ceylonestate.repository;

import com.example.ceylonestate.model.ApplicationStatus;
import com.example.ceylonestate.model.BrokerApplication;
import com.example.ceylonestate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrokerApplicationRepository extends JpaRepository<BrokerApplication, Long> {

    List<BrokerApplication> findAllByOrderByCreatedAtDesc();

    List<BrokerApplication> findByApplicantOrderByCreatedAtDesc(User applicant);

    boolean existsByApplicantAndStatus(User applicant, ApplicationStatus status);

    long countByStatus(ApplicationStatus status);
}
