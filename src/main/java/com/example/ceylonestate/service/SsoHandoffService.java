package com.example.ceylonestate.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Google OAuth login only works via localhost (Google rejects custom local
 * domains as redirect URIs). This service creates a short-lived, single-use
 * token so we can hand the "you're logged in" result off from localhost back
 * to the branded ceylonestates.local domain - similar in spirit to how real
 * single-sign-on systems pass a session between domains.
 *
 * Tokens live in memory only (not the database) since they're only valid for
 * a few seconds and are consumed immediately - no need to persist them.
 */
@Service
public class SsoHandoffService {

    private static final long TOKEN_VALID_SECONDS = 60;

    private final Map<String, HandoffEntry> tokens = new ConcurrentHashMap<>();

    public String createToken(String username) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, new HandoffEntry(username, Instant.now().plusSeconds(TOKEN_VALID_SECONDS)));
        return token;
    }

    /** Consumes the token (single-use) and returns the username if it was valid and not expired. */
    public String consumeToken(String token) {
        HandoffEntry entry = tokens.remove(token); // remove immediately - single use only
        if (entry == null || entry.expiry.isBefore(Instant.now())) {
            return null;
        }
        return entry.username;
    }

    private record HandoffEntry(String username, Instant expiry) {
    }
}
