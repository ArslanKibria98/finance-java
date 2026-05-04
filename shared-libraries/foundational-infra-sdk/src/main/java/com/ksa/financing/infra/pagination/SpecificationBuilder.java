package com.ksa.financing.infra.pagination;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Builds a {@link Specification} from {@link FilterCriteria} list, a global
 * {@code search} keyword, and a mandatory {@code tenantId} predicate.
 *
 * <p><b>Field whitelisting is mandatory</b> — callers MUST provide
 * {@code allowedFilterFields} and {@code searchableFields}. Any client-supplied
 * field name not in the whitelist is silently dropped to prevent SQL/JPQL
 * injection via crafted query strings.
 *
 * <p>Type coercion: values arrive as strings; this builder attempts UUID,
 * Boolean, Number, and Temporal parsing in order. Whatever parses first wins.
 *
 * <p>Usage:
 * <pre>
 * Specification&lt;LoanJpaEntity&gt; spec = SpecificationBuilder.&lt;LoanJpaEntity&gt;builder()
 *     .tenantId(tenantId)
 *     .filters(query.filters())
 *     .allowedFilterFields(Set.of("status", "amount", "customerId", "createdAt"))
 *     .search(query.search())
 *     .searchableFields(Set.of("loanNumber", "referenceCode"))
 *     .build();
 * </pre>
 */
public final class SpecificationBuilder<E> {

    private static final String TENANT_FIELD = "tenantId";

    private UUID tenantId;
    private List<FilterCriteria> filters = List.of();
    private Set<String> allowedFilterFields = Set.of();
    private String search;
    private Set<String> searchableFields = Set.of();

    private SpecificationBuilder() {
    }

    public static <E> SpecificationBuilder<E> builder() {
        return new SpecificationBuilder<>();
    }

    public SpecificationBuilder<E> tenantId(UUID tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public SpecificationBuilder<E> filters(List<FilterCriteria> filters) {
        this.filters = filters == null ? List.of() : filters;
        return this;
    }

    public SpecificationBuilder<E> allowedFilterFields(Set<String> fields) {
        this.allowedFilterFields = fields == null ? Set.of() : fields;
        return this;
    }

    public SpecificationBuilder<E> search(String search) {
        this.search = search;
        return this;
    }

    public SpecificationBuilder<E> searchableFields(Set<String> fields) {
        this.searchableFields = fields == null ? Set.of() : fields;
        return this;
    }

    public Specification<E> build() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tenantId != null) {
                predicates.add(cb.equal(root.get(TENANT_FIELD), tenantId));
            }

            for (FilterCriteria fc : filters) {
                if (!allowedFilterFields.contains(fc.field())) {
                    continue;
                }
                Predicate p = toPredicate(root, cb, fc);
                if (p != null) predicates.add(p);
            }

            if (search != null && !search.isBlank() && !searchableFields.isEmpty()) {
                String like = "%" + search.toLowerCase() + "%";
                List<Predicate> ors = new ArrayList<>();
                for (String f : searchableFields) {
                    Path<String> path = root.get(f);
                    ors.add(cb.like(cb.lower(path), like));
                }
                predicates.add(cb.or(ors.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate toPredicate(jakarta.persistence.criteria.Root<E> root,
                                  jakarta.persistence.criteria.CriteriaBuilder cb,
                                  FilterCriteria fc) {
        Path path = root.get(fc.field());
        Class<?> javaType = path.getJavaType();

        switch (fc.operator()) {
            case IS_NULL:
                return cb.isNull(path);
            case IS_NOT_NULL:
                return cb.isNotNull(path);
            case LIKE: {
                String v = fc.firstValue();
                if (v == null) return null;
                return cb.like(cb.lower(path.as(String.class)), "%" + v.toLowerCase() + "%");
            }
            case IN: {
                List<Object> vals = coerceAll(fc.values(), javaType);
                if (vals.isEmpty()) return null;
                return path.in(vals);
            }
            case NOT_IN: {
                List<Object> vals = coerceAll(fc.values(), javaType);
                if (vals.isEmpty()) return null;
                return cb.not(path.in(vals));
            }
            case BETWEEN: {
                if (fc.values().size() < 2) return null;
                Comparable lo = (Comparable) coerce(fc.values().get(0), javaType);
                Comparable hi = (Comparable) coerce(fc.values().get(1), javaType);
                if (lo == null || hi == null) return null;
                return cb.between(path, lo, hi);
            }
            case EQ: {
                Object v = coerce(fc.firstValue(), javaType);
                return v == null ? cb.isNull(path) : cb.equal(path, v);
            }
            case NEQ: {
                Object v = coerce(fc.firstValue(), javaType);
                return v == null ? cb.isNotNull(path) : cb.notEqual(path, v);
            }
            case GT: {
                Comparable v = (Comparable) coerce(fc.firstValue(), javaType);
                return v == null ? null : cb.greaterThan(path, v);
            }
            case GTE: {
                Comparable v = (Comparable) coerce(fc.firstValue(), javaType);
                return v == null ? null : cb.greaterThanOrEqualTo(path, v);
            }
            case LT: {
                Comparable v = (Comparable) coerce(fc.firstValue(), javaType);
                return v == null ? null : cb.lessThan(path, v);
            }
            case LTE: {
                Comparable v = (Comparable) coerce(fc.firstValue(), javaType);
                return v == null ? null : cb.lessThanOrEqualTo(path, v);
            }
            default:
                return null;
        }
    }

    private List<Object> coerceAll(List<String> raws, Class<?> targetType) {
        List<Object> out = new ArrayList<>(raws.size());
        for (String r : raws) {
            Object v = coerce(r, targetType);
            if (v != null) out.add(v);
        }
        return out;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object coerce(String raw, Class<?> targetType) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        if (targetType == String.class) return s;
        if (targetType == UUID.class) return UUID.fromString(s);
        if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(s);
        if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(s);
        if (targetType == Long.class || targetType == long.class) return Long.parseLong(s);
        if (targetType == Double.class || targetType == double.class) return Double.parseDouble(s);
        if (targetType == BigDecimal.class) return new BigDecimal(s);
        if (targetType == LocalDate.class) return LocalDate.parse(s);
        if (targetType == LocalDateTime.class) return parseLocalDateTime(s);
        if (targetType == OffsetDateTime.class) return parseOffsetDateTime(s);
        if (targetType == Instant.class) return parseInstant(s);

        if (targetType.isEnum()) {
            return Enum.valueOf((Class<Enum>) targetType, s);
        }
        return s;
    }

    private LocalDateTime parseLocalDateTime(String s) {
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException ex) {
            return LocalDate.parse(s).atStartOfDay();
        }
    }

    private OffsetDateTime parseOffsetDateTime(String s) {
        try {
            return OffsetDateTime.parse(s);
        } catch (DateTimeParseException ex) {
            return LocalDate.parse(s).atStartOfDay().atOffset(ZoneOffset.UTC);
        }
    }

    private Instant parseInstant(String s) {
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException ex) {
            return LocalDate.parse(s).atStartOfDay().toInstant(ZoneOffset.UTC);
        }
    }
}
