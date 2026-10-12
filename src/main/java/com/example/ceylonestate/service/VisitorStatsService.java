package com.example.ceylonestate.service;

import com.example.ceylonestate.model.PageView;
import com.example.ceylonestate.repository.PageViewRepository;
import com.example.ceylonestate.repository.PropertyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Turns the raw page-view rows into the dashboard numbers. The last 30 days are
 * loaded once and counted in Java - simple, and plenty fast for this project.
 */
@Service
public class VisitorStatsService {

    private static final Pattern LISTING_PATH = Pattern.compile("^/properties/(\\d+)$");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

    private final PageViewRepository pageViewRepository;
    private final PropertyRepository propertyRepository;

    public VisitorStatsService(PageViewRepository pageViewRepository, PropertyRepository propertyRepository) {
        this.pageViewRepository = pageViewRepository;
        this.propertyRepository = propertyRepository;
    }

    /** @param chartDays how many days the bar chart and the top lists cover (7-30) */
    public VisitorStats compute(int chartDays) {
        int days = Math.max(7, Math.min(30, chartDays));
        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime start7 = today.minusDays(6).atStartOfDay();
        LocalDateTime start30 = today.minusDays(29).atStartOfDay();
        LocalDateTime startChart = today.minusDays(days - 1L).atStartOfDay();

        List<PageView> views = pageViewRepository.findByViewedAtAfter(start30.minusNanos(1));

        VisitorStats stats = new VisitorStats();
        stats.setViewsToday(count(views, startToday));
        stats.setViewsLast7(count(views, start7));
        stats.setViewsLast30(views.size());
        stats.setVisitorsToday(unique(views, startToday));
        stats.setVisitorsLast7(unique(views, start7));
        stats.setVisitorsLast30(unique(views, start30));

        // Bar chart: one column per day, oldest first.
        List<VisitorStats.DayCount> daily = new ArrayList<>();
        long maxViews = 1;
        Map<LocalDate, List<PageView>> byDay = views.stream()
                .collect(Collectors.groupingBy(v -> v.getViewedAt().toLocalDate()));
        for (int i = days - 1; i >= 0; i--) {
            List<PageView> dayViews = byDay.getOrDefault(today.minusDays(i), List.of());
            maxViews = Math.max(maxViews, dayViews.size());
        }
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            List<PageView> dayViews = byDay.getOrDefault(day, List.of());
            long visitors = dayViews.stream().map(PageView::getVisitorId).distinct().count();
            int height = (int) Math.round(dayViews.size() * 100.0 / maxViews);
            daily.add(new VisitorStats.DayCount(day.format(DAY_LABEL), dayViews.size(), visitors, height));
        }
        stats.setDaily(daily);

        List<PageView> inChart = views.stream().filter(v -> !v.getViewedAt().isBefore(startChart)).toList();

        stats.setTopPages(top(inChart.stream().map(v -> pageLabel(v.getPath())).toList(), 6));
        stats.setDevices(top(inChart.stream().map(v -> v.getDevice() == null ? "Desktop" : v.getDevice()).toList(), 3));
        stats.setReferrers(top(inChart.stream().map(v -> v.getReferrer() == null ? "Direct" : v.getReferrer()).toList(), 6));
        stats.setTopListings(topListings(inChart, 5));
        return stats;
    }

    public List<PageView> recentViews() {
        return pageViewRepository.findTop20ByOrderByViewedAtDesc();
    }

    // ---------------------------------------------------------------------

    private long count(List<PageView> views, LocalDateTime since) {
        return views.stream().filter(v -> !v.getViewedAt().isBefore(since)).count();
    }

    private long unique(List<PageView> views, LocalDateTime since) {
        Set<String> ids = new HashSet<>();
        for (PageView v : views) {
            if (!v.getViewedAt().isBefore(since)) {
                ids.add(v.getVisitorId());
            }
        }
        return ids.size();
    }

    /** Groups the individual listing pages (/properties/12) under one friendly label. */
    private String pageLabel(String path) {
        if (path.equals("/")) {
            return "Home";
        }
        if (LISTING_PATH.matcher(path).matches()) {
            return "Listing pages";
        }
        String name = path.startsWith("/") ? path.substring(1) : path;
        return name.isEmpty() ? "Home" : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private List<VisitorStats.Counted> top(List<String> labels, int limit) {
        Map<String, Long> counts = labels.stream()
                .collect(Collectors.groupingBy(l -> l, LinkedHashMap::new, Collectors.counting()));
        long total = Math.max(1, labels.size());
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new VisitorStats.Counted(e.getKey(), e.getValue(), (int) Math.round(e.getValue() * 100.0 / total), null))
                .toList();
    }

    private List<VisitorStats.Counted> topListings(List<PageView> views, int limit) {
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (PageView v : views) {
            Matcher m = LISTING_PATH.matcher(v.getPath());
            if (m.matches()) {
                counts.merge(Long.parseLong(m.group(1)), 1L, Long::sum);
            }
        }
        long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(1);
        List<VisitorStats.Counted> result = new ArrayList<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .forEach(e -> propertyRepository.findById(e.getKey()).ifPresent(p ->
                        result.add(new VisitorStats.Counted(p.getTitle(), e.getValue(),
                                (int) Math.round(e.getValue() * 100.0 / max), p.getId()))));
        return result;
    }
}
