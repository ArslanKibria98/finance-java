package com.ksa.financing.ledger.application.service.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * Shared helper for extracting BigDecimal values from raw {@code Object[]} aggregate
 * results returned by JPA queries (e.g., {@code SELECT SUM(a), SUM(b) ...}).
 *
 * <p>Handles edge cases observed across PostgreSQL drivers and Hibernate versions:
 * <ul>
 *     <li>BigDecimal / Number direct values</li>
 *     <li>String values with malformed scientific notation (BigDecimal parse failure)</li>
 *     <li>Composite tuple format {@code "(value1,value2)"} returned as a single element</li>
 *     <li>Null rows / missing indices / blank strings</li>
 * </ul>
 */
@Slf4j
public final class AggregateValueUtil {

    private AggregateValueUtil() {}

    public static BigDecimal valueAt(Object[] values, int index) {
        // Hibernate 6 sometimes wraps a multi-column scalar tuple as Object[][] (one row,
        // wrapped). Detect length==1 with inner Object[] and unwrap so callers see the flat row.
        if (values != null && values.length == 1 && values[0] instanceof Object[] inner) {
            values = inner;
        }
        if (values == null || values.length <= index || values[index] == null) {
            if (values != null && values.length == 1 && values[0] != null) {
                BigDecimal composite = parseCompositeTuple(values[0].toString(), index);
                if (composite != null) {
                    return composite;
                }
            }
            return BigDecimal.ZERO;
        }
        Object raw = values[index];
        if (raw instanceof BigDecimal bd) {
            return bd;
        }
        if (raw instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        String asString = raw.toString().trim();
        if (asString.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(asString);
        } catch (NumberFormatException ex) {
            BigDecimal composite = parseCompositeTuple(asString, index);
            if (composite != null) {
                return composite;
            }
            log.warn("Unable to parse numeric aggregate value '{}' at index {}. Falling back to 0.", asString, index);
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal parseCompositeTuple(String value, int index) {
        String text = value == null ? "" : value.trim();
        if (!text.startsWith("(") || !text.endsWith(")")) {
            return null;
        }
        String body = text.substring(1, text.length() - 1);
        String[] parts = body.split(",");
        if (parts.length <= index) {
            return BigDecimal.ZERO;
        }
        String token = parts[index].trim();
        if (token.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(token);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }
}
