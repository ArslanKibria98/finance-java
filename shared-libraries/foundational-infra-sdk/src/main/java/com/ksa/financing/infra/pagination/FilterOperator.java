package com.ksa.financing.infra.pagination;

public enum FilterOperator {
    EQ,
    NEQ,
    LIKE,
    GT,
    GTE,
    LT,
    LTE,
    IN,
    NOT_IN,
    BETWEEN,
    IS_NULL,
    IS_NOT_NULL;

    public static FilterOperator parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return EQ;
        }
        try {
            return FilterOperator.valueOf(raw.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            return EQ;
        }
    }
}
