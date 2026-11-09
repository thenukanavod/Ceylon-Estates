package com.example.ceylonestate.member6_it26100001.service;

import com.example.ceylonestate.member6_it26100001.model.AdminActivity;
import com.example.ceylonestate.member6_it26100001.repository.AdminActivityRepository;
import org.springframework.stereotype.Service;

/** Writes the admin audit log (who did what). */
@Service
public class AuditLogService {

    private final AdminActivityRepository repository;

    public AuditLogService(AdminActivityRepository repository) {
        this.repository = repository;
    }

    public void log(String adminUsername, String action) {
        try {
            repository.save(new AdminActivity(adminUsername, action));
        } catch (RuntimeException e) {
            // The audit log must never break the action it is recording.
        }
    }
}
