package com.example.ceylonestate.model;

/** Where a "become a broker" application is: waiting for an admin, then approved or rejected. */
public enum ApplicationStatus {
    PENDING("Pending", "warning"),
    APPROVED("Approved", "success"),
    REJECTED("Rejected", "danger");

    private final String label;
    private final String badgeClass; // Bootstrap contextual colour: text-bg-<badgeClass>

    ApplicationStatus(String label, String badgeClass) {
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
