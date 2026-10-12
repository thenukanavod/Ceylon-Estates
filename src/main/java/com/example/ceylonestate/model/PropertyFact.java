package com.example.ceylonestate.model;

/**
 * One "label: value" line in a property's key-facts grid (e.g. Bedrooms: 3).
 * Plain object, not a database entity - each Property subclass builds its own
 * list of these, which is how the detail page shows different facts for
 * lands, houses and apartments without any if/else in the template.
 */
public class PropertyFact {

    private final String labelKey; // key in messages.properties
    private final String value;
    private final String icon;     // Bootstrap Icons class, e.g. "bi-door-closed"

    public PropertyFact(String labelKey, String value, String icon) {
        this.labelKey = labelKey;
        this.value = value;
        this.icon = icon;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public String getValue() {
        return value;
    }

    public String getIcon() {
        return icon;
    }
}
