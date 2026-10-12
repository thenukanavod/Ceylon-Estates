package com.example.ceylonestate.model;

import java.util.List;

/**
 * Where a listing is in its life: submitted by a seller (PENDING), reviewed
 * by an admin (FOR_SALE / FOR_RENT go live, REJECTED is sent back), and
 * finally closed out (SOLD / RENTED), at which point it joins the
 * "Completed Projects" library.
 */
public enum ListingStatus {
    PENDING("Pending review", "warning"),
    FOR_SALE("For sale", "success"),
    FOR_RENT("For rent", "info"),
    SOLD("Sold", "secondary"),
    RENTED("Rented", "secondary"),
    REJECTED("Rejected", "danger");

    /** Statuses visible to buyers browsing the site. */
    public static final List<ListingStatus> LIVE = List.of(FOR_SALE, FOR_RENT);

    /** Statuses shown in the Completed Projects library. */
    public static final List<ListingStatus> COMPLETED = List.of(SOLD, RENTED);

    private final String label;
    private final String badgeClass; // Bootstrap contextual colour: bg-<badgeClass>

    ListingStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public boolean isLive() {
        return LIVE.contains(this);
    }

    public boolean isCompleted() {
        return COMPLETED.contains(this);
    }
}
