package com.ksa.financing.ledger.application.service.util;

/**
 * Helper for case-insensitive substring search across multiple fields.
 * Used by report services to filter in-memory lists by an optional search term.
 */
public final class SearchFilterUtil {

    private SearchFilterUtil() {}

    /**
     * Returns {@code true} if {@code search} is null/blank (no filter), or if any
     * of the provided field values contains {@code search} (case-insensitive).
     */
    public static boolean matchesSearch(String search, String... fields) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String needle = search.toLowerCase();
        if (fields == null) {
            return false;
        }
        for (String f : fields) {
            if (f != null && f.toLowerCase().contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
