package com.example.ceylonestate.config;

import com.example.ceylonestate.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes the admin audit log: every successful change made under /admin/**
 * (a POST that ends in a redirect) is recorded with the admin's username.
 * One central place, so the individual admin controllers stay clean.
 */
@Component
public class AdminAuditInterceptor implements HandlerInterceptor {

    private static final Map<Pattern, String> ACTIONS = new LinkedHashMap<>();

    static {
        add("^/admin/listings/(\\d+)/approve$", "Approved listing #%s");
        add("^/admin/listings/(\\d+)/reject$", "Rejected listing #%s");
        add("^/admin/listings/(\\d+)/delete$", "Deleted listing #%s");
        add("^/admin/brokers$", "Added a broker");
        add("^/admin/brokers/(\\d+)$", "Updated broker #%s");
        add("^/admin/brokers/(\\d+)/delete$", "Deleted broker #%s");
        add("^/admin/local-info$", "Added a district guide");
        add("^/admin/local-info/(\\d+)$", "Updated district guide #%s");
        add("^/admin/local-info/(\\d+)/delete$", "Deleted district guide #%s");
        add("^/admin/local-info/import$", "Imported district guides from a CSV file");
        add("^/admin/messages/(\\d+)/reply$", "Replied to message #%s");
        add("^/admin/messages/(\\d+)/status$", "Changed the status of message #%s");
        add("^/admin/messages/(\\d+)/delete$", "Deleted message #%s");
        add("^/admin/bookings/(\\d+)/confirm$", "Confirmed viewing booking #%s");
        add("^/admin/bookings/(\\d+)/decline$", "Declined viewing booking #%s");
        add("^/admin/bookings/(\\d+)/delete$", "Deleted viewing booking #%s");
        add("^/admin/broker-applications/(\\d+)/approve$", "Approved broker application #%s");
        add("^/admin/broker-applications/(\\d+)/reject$", "Rejected broker application #%s");
        add("^/admin/broker-applications/(\\d+)/delete$", "Deleted broker application #%s");
        add("^/admin/articles$", "Added an article");
        add("^/admin/articles/(\\d+)$", "Edited article #%s");
        add("^/admin/articles/(\\d+)/delete$", "Deleted article #%s");
        add("^/admin/users/new$", "Created a new admin account");
        add("^/admin/users/(\\d+)/promote$", "Made user #%s an admin");
        add("^/admin/users/(\\d+)/demote$", "Removed admin rights from user #%s");
        add("^/admin/users/(\\d+)/delete$", "Deleted user #%s");
    }

    private static void add(String regex, String text) {
        ACTIONS.put(Pattern.compile(regex), text);
    }

    private final AuditLogService auditLogService;

    public AdminAuditInterceptor(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!"POST".equals(request.getMethod()) || ex != null) {
            return;
        }
        int status = response.getStatus();
        if (status != 302 && status != 303) {
            return; // validation errors re-render the form with 200 - nothing changed
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }
        // strip the app's context path (/ceylonestates) so the patterns below match
        String path = request.getRequestURI().substring(request.getContextPath().length());
        for (Map.Entry<Pattern, String> entry : ACTIONS.entrySet()) {
            Matcher m = entry.getKey().matcher(path);
            if (m.matches()) {
                String action = m.groupCount() > 0 ? String.format(entry.getValue(), m.group(1)) : entry.getValue();
                auditLogService.log(auth.getName(), action);
                return;
            }
        }
    }
}
