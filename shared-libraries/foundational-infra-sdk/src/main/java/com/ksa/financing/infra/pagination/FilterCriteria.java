package com.ksa.financing.infra.pagination;

import java.util.List;

/**
 * Single filter expression: {@code field}, {@code operator}, and one or more {@code values}.
 * <p>Wire format (query string): {@code filter=field:op:value} or {@code filter=field:op:v1,v2}
 * <ul>
 *   <li>{@code filter=status:eq:ACTIVE}</li>
 *   <li>{@code filter=amount:gte:1000}</li>
 *   <li>{@code filter=createdAt:between:2026-01-01,2026-12-31}</li>
 *   <li>{@code filter=productType:in:MURABAHA,TAWARRUQ}</li>
 *   <li>{@code filter=deletedAt:is_null}</li>
 * </ul>
 */
public record FilterCriteria(String field, FilterOperator operator, List<String> values) {

    public String firstValue() {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    public static FilterCriteria parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split(":", 3);
        if (parts.length < 2) {
            return null;
        }
        String field = parts[0].trim();
        if (field.isEmpty()) {
            return null;
        }
        FilterOperator op = FilterOperator.parse(parts[1]);

        List<String> values;
        if (op == FilterOperator.IS_NULL || op == FilterOperator.IS_NOT_NULL) {
            values = List.of();
        } else if (parts.length < 3) {
            return null;
        } else {
            values = List.of(parts[2].split(","));
        }
        return new FilterCriteria(field, op, values);
    }
}
