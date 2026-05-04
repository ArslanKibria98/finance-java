package com.ksa.financing.infra.pagination;

import org.springframework.data.domain.Sort;

public enum SortDirection {
    ASC,
    DESC;

    public Sort.Direction toSpring() {
        return this == ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    public static SortDirection parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DESC;
        }
        try {
            return SortDirection.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return DESC;
        }
    }
}
