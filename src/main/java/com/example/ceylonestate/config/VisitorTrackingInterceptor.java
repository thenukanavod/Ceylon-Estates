package com.example.ceylonestate.config;

import com.example.ceylonestate.model.PageView;
import com.example.ceylonestate.repository.PageViewRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Counts page views of the public pages for the admin "Visitors" statistics.
 * Only successful GET requests to real pages are counted (not images, CSS, the
 * admin area or known bots). A random cookie id tells returning visitors apart;
 * no names or IP addresses are stored.
 */
@Component
public class VisitorTrackingInterceptor implements HandlerInterceptor {

    static final String COOKIE_NAME = "ce_vid";
    private static final String ATTR_VISITOR = "ce.visitorId";
    private static final Pattern BOT = Pattern.compile("bot|crawl|spider|slurp|preview|monitor|curl|wget|python|java/", Pattern.CASE_INSENSITIVE);
    private static final List<String> TRACKED_PREFIXES =
            List.of("/properties", "/projects", "/brokers", "/local-info", "/contact", "/sell");

    private final PageViewRepository pageViewRepository;

    public VisitorTrackingInterceptor(PageViewRepository pageViewRepository) {
        this.pageViewRepository = pageViewRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (shouldTrack(request, handler)) {
            String visitorId = readCookie(request);
            if (visitorId == null) {
                visitorId = UUID.randomUUID().toString();
                Cookie cookie = new Cookie(COOKIE_NAME, visitorId);
                cookie.setPath("/");
                cookie.setHttpOnly(true);
                cookie.setMaxAge(365 * 24 * 60 * 60);
                response.addCookie(cookie);
            }
            request.setAttribute(ATTR_VISITOR, visitorId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object visitor = request.getAttribute(ATTR_VISITOR);
        if (visitor == null || ex != null || response.getStatus() >= 400) {
            return;
        }
        try {
            String path = request.getRequestURI();
            pageViewRepository.save(new PageView(
                    path.length() > 255 ? path.substring(0, 255) : path,
                    visitor.toString(),
                    LocalDateTime.now(),
                    deviceOf(request.getHeader("User-Agent")),
                    referrerOf(request)));
        } catch (RuntimeException e) {
            // Statistics must never break a page.
        }
    }

    private boolean shouldTrack(HttpServletRequest request, Object handler) {
        if (!"GET".equals(request.getMethod()) || !(handler instanceof HandlerMethod)) {
            return false;
        }
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || BOT.matcher(userAgent).find()) {
            return false;
        }
        String path = request.getRequestURI();
        if (path.equals("/")) {
            return true;
        }
        return TRACKED_PREFIXES.stream().anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    private String readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null && cookie.getValue().length() <= 40) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String deviceOf(String userAgent) {
        if (userAgent == null) {
            return "Desktop";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("ipad") || ua.contains("tablet")) {
            return "Tablet";
        }
        if (ua.contains("mobi") || ua.contains("android") || ua.contains("iphone")) {
            return "Mobile";
        }
        return "Desktop";
    }

    /** The host the visitor came from, or null for direct visits and our own pages. */
    private String referrerOf(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            String host = URI.create(referer).getHost();
            if (host == null || host.equalsIgnoreCase(request.getServerName())) {
                return null;
            }
            return host.length() > 120 ? host.substring(0, 120) : host;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
