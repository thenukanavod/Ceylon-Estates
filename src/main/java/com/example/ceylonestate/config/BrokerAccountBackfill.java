package com.example.ceylonestate.config;

import com.example.ceylonestate.model.ApplicationStatus;
import com.example.ceylonestate.model.BrokerApplication;
import com.example.ceylonestate.repository.BrokerApplicationRepository;
import com.example.ceylonestate.repository.BrokerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Approving an application now links the applicant's account to the new broker automatically.
 * Applications that were approved before that change have a broker but no login. On start-up this
 * links them (matched by the application's email), so those applicants get their Broker inbox too.
 * It does nothing once everything is linked.
 */
@Component
public class BrokerAccountBackfill implements CommandLineRunner {

    private final BrokerApplicationRepository applications;
    private final BrokerRepository brokers;

    public BrokerAccountBackfill(BrokerApplicationRepository applications, BrokerRepository brokers) {
        this.applications = applications;
        this.brokers = brokers;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (BrokerApplication app : applications.findAll()) {
            if (app.getStatus() != ApplicationStatus.APPROVED) {
                continue;
            }
            if (brokers.findByAccount(app.getApplicant()).isPresent()) {
                continue;
            }
            brokers.findFirstByEmailIgnoreCaseAndAccountIsNull(app.getEmail()).ifPresent(broker -> {
                broker.linkAccount(app.getApplicant());
                brokers.save(broker);
            });
        }
    }
}
