package com.example.ceylonestate.member4_it26100746.repository;

import com.example.ceylonestate.member4_it26100746.model.ApplicationStatus;
import com.example.ceylonestate.member4_it26100746.model.BrokerApplication;
import com.example.ceylonestate.member1_it26100453.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrokerApplicationRepository extends JpaRepository<BrokerApplication, Long> {

    List<BrokerApplication> findAllByOrderByCreatedAtDesc();

    List<BrokerApplication> findByApplicantOrderByCreatedAtDesc(User applicant);

    boolean existsByApplicantAndStatus(User applicant, ApplicationStatus status);

    long countByStatus(ApplicationStatus status);
}
