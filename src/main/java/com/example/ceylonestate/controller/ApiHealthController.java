package com.example.ceylonestate.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple REST endpoint to confirm the backend is alive.
 * Visit: http://localhost:8080/api/health
 */
@RestController
public class ApiHealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "message", "Spring Boot backend is running"
        );
    }
}
