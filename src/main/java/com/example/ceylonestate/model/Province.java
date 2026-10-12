package com.example.ceylonestate.model;

/**
 * Sri Lanka's nine provinces. The "key" matches the data-province values the
 * homepage's broker filter already uses, and the messageKey matches the
 * existing province.* entries in messages.properties - so nothing new is
 * needed on the translation side.
 */
public enum Province {
    WESTERN("western", "province.western"),
    CENTRAL("central", "province.central"),
    SOUTHERN("southern", "province.southern"),
    NORTHERN("northern", "province.northern"),
    EASTERN("eastern", "province.eastern"),
    NORTH_WESTERN("northWestern", "province.northWestern"),
    NORTH_CENTRAL("northCentral", "province.northCentral"),
    UVA("uva", "province.uva"),
    SABARAGAMUWA("sabaragamuwa", "province.sabaragamuwa");

    private final String key;
    private final String messageKey;

    Province(String key, String messageKey) {
        this.key = key;
        this.messageKey = messageKey;
    }

    public String getKey() {
        return key;
    }

    public String getMessageKey() {
        return messageKey;
    }

    /** Looks a province up by its homepage key (e.g. "northWestern"); null if unknown. */
    public static Province fromKey(String key) {
        if (key == null) {
            return null;
        }
        for (Province p : values()) {
            if (p.key.equalsIgnoreCase(key.trim())) {
                return p;
            }
        }
        return null;
    }
}
