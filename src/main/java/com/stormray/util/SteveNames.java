package com.stormray.util;

/** Pure rule for which player-head owners count as "Steve". */
public final class SteveNames {
    private SteveNames() {
    }

    /**
     * @param name  owner name, may be null or blank
     * @param hasId whether the owner profile carries a UUID
     */
    public static boolean isSteve(String name, boolean hasId) {
        if (name == null || name.isBlank()) {
            return !hasId;
        }
        return "Steve".equalsIgnoreCase(name) || "MHF_Steve".equalsIgnoreCase(name);
    }
}
