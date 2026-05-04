package com.ksa.financing.infra.pagination;

import org.springframework.data.domain.Sort;

public record SortField(String field, SortDirection direction) {

    public Sort.Order toOrder() {
        return new Sort.Order(direction.toSpring(), field);
    }

    public static SortField parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split(",", 2);
        String field = parts[0].trim();
        if (field.isEmpty()) {
            return null;
        }
        SortDirection dir = parts.length > 1 ? SortDirection.parse(parts[1]) : SortDirection.DESC;
        return new SortField(field, dir);
    }
}
