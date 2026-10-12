package com.example.ceylonestate.service;

import java.util.List;

/** The numbers shown on the admin dashboard and the Visitors page. */
public class VisitorStats {

    /** One label with a count and its share of the total (0-100) - used for bar rows. */
    public static class Counted {
        private final String label;
        private final long count;
        private final int percent;
        private final Long propertyId; // only set for "most viewed listings"

        public Counted(String label, long count, int percent, Long propertyId) {
            this.label = label;
            this.count = count;
            this.percent = percent;
            this.propertyId = propertyId;
        }

        public String getLabel() { return label; }
        public long getCount() { return count; }
        public int getPercent() { return percent; }
        public Long getPropertyId() { return propertyId; }
    }

    /** One day of the bar chart. */
    public static class DayCount {
        private final String label;
        private final long views;
        private final long visitors;
        private final int heightPercent;

        public DayCount(String label, long views, long visitors, int heightPercent) {
            this.label = label;
            this.views = views;
            this.visitors = visitors;
            this.heightPercent = heightPercent;
        }

        public String getLabel() { return label; }
        public long getViews() { return views; }
        public long getVisitors() { return visitors; }
        public int getHeightPercent() { return heightPercent; }
    }

    private long viewsToday;
    private long viewsLast7;
    private long viewsLast30;
    private long visitorsToday;
    private long visitorsLast7;
    private long visitorsLast30;
    private List<DayCount> daily;
    private List<Counted> topPages;
    private List<Counted> topListings;
    private List<Counted> devices;
    private List<Counted> referrers;

    public long getViewsToday() { return viewsToday; }
    public void setViewsToday(long viewsToday) { this.viewsToday = viewsToday; }

    public long getViewsLast7() { return viewsLast7; }
    public void setViewsLast7(long viewsLast7) { this.viewsLast7 = viewsLast7; }

    public long getViewsLast30() { return viewsLast30; }
    public void setViewsLast30(long viewsLast30) { this.viewsLast30 = viewsLast30; }

    public long getVisitorsToday() { return visitorsToday; }
    public void setVisitorsToday(long visitorsToday) { this.visitorsToday = visitorsToday; }

    public long getVisitorsLast7() { return visitorsLast7; }
    public void setVisitorsLast7(long visitorsLast7) { this.visitorsLast7 = visitorsLast7; }

    public long getVisitorsLast30() { return visitorsLast30; }
    public void setVisitorsLast30(long visitorsLast30) { this.visitorsLast30 = visitorsLast30; }

    public List<DayCount> getDaily() { return daily; }
    public void setDaily(List<DayCount> daily) { this.daily = daily; }

    public List<Counted> getTopPages() { return topPages; }
    public void setTopPages(List<Counted> topPages) { this.topPages = topPages; }

    public List<Counted> getTopListings() { return topListings; }
    public void setTopListings(List<Counted> topListings) { this.topListings = topListings; }

    public List<Counted> getDevices() { return devices; }
    public void setDevices(List<Counted> devices) { this.devices = devices; }

    public List<Counted> getReferrers() { return referrers; }
    public void setReferrers(List<Counted> referrers) { this.referrers = referrers; }
}
