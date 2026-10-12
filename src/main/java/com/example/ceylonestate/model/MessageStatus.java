package com.example.ceylonestate.model;

/** Where a contact message is in the admin inbox. */
public enum MessageStatus {
    NEW("New", "text-bg-danger"),
    READ("Read", "text-bg-secondary"),
    ARCHIVED("Archived", "text-bg-light border");

    private final String label;
    private final String badgeClass;

    MessageStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() { return label; }

    public String getBadgeClass() { return badgeClass; }
}
