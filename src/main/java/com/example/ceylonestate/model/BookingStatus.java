package com.example.ceylonestate.model;

/**
 * Where a viewing request is in its life: asked for by the buyer (REQUESTED),
 * then confirmed or declined by an admin. Rescheduling puts it back to REQUESTED.
 */
public enum BookingStatus {
    REQUESTED("Requested", "warning"),
    CONFIRMED("Confirmed", "success"),
    DECLINED("Declined", "danger");

    private final String label;
    private final String badgeClass; // Bootstrap contextual colour: bg-<badgeClass>

    BookingStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
