package com.example.ceylonestate.service;

import com.example.ceylonestate.model.AdminActivity;
import com.example.ceylonestate.repository.AdminActivityRepository;
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
