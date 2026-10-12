package com.example.ceylonestate.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force protection: after 5 wrong passwords in a row an account is locked
 * for 15 minutes. Kept in memory, so it resets when the application restarts.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_TIME = Duration.ofMinutes(15);

    private static class State {
        int failures;
        Instant lockedUntil;
    }

    private final Map<String, State> attempts = new ConcurrentHashMap<>();

    public void recordFailure(String username) {
        if (username == null) {
            return;
        }
        State state = attempts.computeIfAbsent(key(username), k -> new State());
        synchronized (state) {
            // A finished lock starts a fresh count.
            if (state.lockedUntil != null && Instant.now().isAfter(state.lockedUntil)) {
                state.failures = 0;
                state.lockedUntil = null;
            }
            state.failures++;
            if (state.failures >= MAX_FAILURES) {
                state.lockedUntil = Instant.now().plus(LOCK_TIME);
            }
        }
    }

    public void recordSuccess(String username) {
        if (username != null) {
            attempts.remove(key(username));
        }
    }

    public boolean isLocked(String username) {
        if (username == null) {
            return false;
        }
        State state = attempts.get(key(username));
        if (state == null) {
            return false;
        }
        synchronized (state) {
            return state.lockedUntil != null && Instant.now().isBefore(state.lockedUntil);
        }
    }

    private String key(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
