package com.example.ceylonestate.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A small lookup of well-known towns and neighbourhoods and the district each
 * one sits in, so that searching "Negombo" or "Nugegoda" on the Get Local
 * Info box lands on the right district. (District.fromText already handles
 * district names themselves.)
 */
public final class TownDirectory {

    private static final Map<String, District> TOWNS = new LinkedHashMap<>();

    static {
        // Colombo district
        for (String town : new String[]{"nugegoda", "rajagiriya", "maharagama", "dehiwala", "mount lavinia",
                "moratuwa", "battaramulla", "kotte", "sri jayawardenepura", "kollupitiya", "bambalapitiya",
                "wellawatte", "homagama", "piliyandala", "kaduwela", "malabe", "nawala", "thalawathugoda",
                "borella", "pettah", "fort", "havelock"}) {
            TOWNS.put(town, District.COLOMBO);
        }
        // Gampaha district
        for (String town : new String[]{"negombo", "ja-ela", "ja ela", "wattala", "ragama", "kelaniya",
                "katunayake", "minuwangoda", "divulapitiya", "nittambuwa", "kiribathgoda"}) {
            TOWNS.put(town, District.GAMPAHA);
        }
        // Kalutara district
        for (String town : new String[]{"panadura", "horana", "beruwala", "aluthgama", "wadduwa", "matugama"}) {
            TOWNS.put(town, District.KALUTARA);
        }
        // Central province
        for (String town : new String[]{"peradeniya", "katugastota", "gampola", "nawalapitiya", "digana"}) {
            TOWNS.put(town, District.KANDY);
        }
        for (String town : new String[]{"dambulla", "sigiriya", "rattota"}) {
            TOWNS.put(town, District.MATALE);
        }
        for (String town : new String[]{"hatton", "talawakele", "nanu oya", "lindula"}) {
            TOWNS.put(town, District.NUWARA_ELIYA);
        }
        // Southern province
        for (String town : new String[]{"hikkaduwa", "unawatuna", "bentota", "ambalangoda", "baddegama"}) {
            TOWNS.put(town, District.GALLE);
        }
        for (String town : new String[]{"weligama", "mirissa", "dikwella", "akuressa", "kamburupitiya"}) {
            TOWNS.put(town, District.MATARA);
        }
        for (String town : new String[]{"tangalle", "tissamaharama", "tissa", "ambalantota", "beliatta"}) {
            TOWNS.put(town, District.HAMBANTOTA);
        }
        // North and east
        TOWNS.put("nallur", District.JAFFNA);
        TOWNS.put("chavakachcheri", District.JAFFNA);
        TOWNS.put("pasikudah", District.BATTICALOA);
        TOWNS.put("kalkudah", District.BATTICALOA);
        TOWNS.put("arugam bay", District.AMPARA);
        TOWNS.put("kalmunai", District.AMPARA);
        TOWNS.put("nilaveli", District.TRINCOMALEE);
        TOWNS.put("uppuveli", District.TRINCOMALEE);
        // North western and north central
        TOWNS.put("kuliyapitiya", District.KURUNEGALA);
        TOWNS.put("chilaw", District.PUTTALAM);
        TOWNS.put("kalpitiya", District.PUTTALAM);
        TOWNS.put("giritale", District.POLONNARUWA);
        TOWNS.put("mihintale", District.ANURADHAPURA);
        // Uva and Sabaragamuwa
        for (String town : new String[]{"ella", "bandarawela", "haputale", "hali-ela", "hali ela"}) {
            TOWNS.put(town, District.BADULLA);
        }
        TOWNS.put("wellawaya", District.MONARAGALA);
        TOWNS.put("embilipitiya", District.RATNAPURA);
        TOWNS.put("balangoda", District.RATNAPURA);
        TOWNS.put("mawanella", District.KEGALLE);
    }

    private TownDirectory() {
        // static lookup only
    }

    /** The district a town or neighbourhood name belongs to, or null if it isn't in the list. */
    public static District find(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String needle = text.trim().toLowerCase();
        District exact = TOWNS.get(needle);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, District> entry : TOWNS.entrySet()) {
            if (needle.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
