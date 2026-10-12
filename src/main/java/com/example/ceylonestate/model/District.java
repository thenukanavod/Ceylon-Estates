package com.example.ceylonestate.model;

import java.util.Arrays;
import java.util.List;

/**
 * All 25 administrative districts of Sri Lanka, each knowing which province
 * it belongs to. Everything location-based in the app (listings, brokers,
 * local info) is organised by this enum.
 */
public enum District {
    COLOMBO("Colombo", Province.WESTERN),
    GAMPAHA("Gampaha", Province.WESTERN),
    KALUTARA("Kalutara", Province.WESTERN),

    KANDY("Kandy", Province.CENTRAL),
    MATALE("Matale", Province.CENTRAL),
    NUWARA_ELIYA("Nuwara Eliya", Province.CENTRAL),

    GALLE("Galle", Province.SOUTHERN),
    MATARA("Matara", Province.SOUTHERN),
    HAMBANTOTA("Hambantota", Province.SOUTHERN),

    JAFFNA("Jaffna", Province.NORTHERN),
    KILINOCHCHI("Kilinochchi", Province.NORTHERN),
    MANNAR("Mannar", Province.NORTHERN),
    VAVUNIYA("Vavuniya", Province.NORTHERN),
    MULLAITIVU("Mullaitivu", Province.NORTHERN),

    BATTICALOA("Batticaloa", Province.EASTERN),
    AMPARA("Ampara", Province.EASTERN),
    TRINCOMALEE("Trincomalee", Province.EASTERN),

    KURUNEGALA("Kurunegala", Province.NORTH_WESTERN),
    PUTTALAM("Puttalam", Province.NORTH_WESTERN),

    ANURADHAPURA("Anuradhapura", Province.NORTH_CENTRAL),
    POLONNARUWA("Polonnaruwa", Province.NORTH_CENTRAL),

    BADULLA("Badulla", Province.UVA),
    MONARAGALA("Monaragala", Province.UVA),

    RATNAPURA("Ratnapura", Province.SABARAGAMUWA),
    KEGALLE("Kegalle", Province.SABARAGAMUWA);

    private final String label;
    private final Province province;

    District(String label, Province province) {
        this.label = label;
        this.province = province;
    }

    public String getLabel() {
        return label;
    }

    public Province getProvince() {
        return province;
    }

    /** All districts that belong to the given province. */
    public static List<District> inProvince(Province province) {
        return Arrays.stream(values()).filter(d -> d.province == province).toList();
    }

    /**
     * Forgiving lookup used by the search boxes: matches the display name
     * ("Nuwara Eliya"), the enum name ("NUWARA_ELIYA") or any text that
     * contains a district name ("Colombo 03" -> COLOMBO). Returns null if
     * nothing matches.
     */
    public static District fromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String needle = text.trim().toLowerCase();
        for (District d : values()) {
            if (d.label.equalsIgnoreCase(needle) || d.name().equalsIgnoreCase(needle)) {
                return d;
            }
        }
        for (District d : values()) {
            if (needle.contains(d.label.toLowerCase())) {
                return d;
            }
        }
        return null;
    }
}
