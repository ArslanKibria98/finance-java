package com.ksa.financing.infra.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resolved pagination request. Built by {@link PageQueryArgumentResolver} from query
 * string parameters, then passed down through use cases to repositories.
 *
 * <p>Use cases should treat this as an opaque value; convert to {@link Pageable} via
 * {@link #toPageable()} when calling Spring Data, and to a {@code Specification} via
 * {@code SpecificationBuilder.build(...)} for dynamic filters.
 */
public record PageQuery(
        int page,
        int size,
        List<SortField> sort,
        List<FilterCriteria> filters,
        String search,
        String pageToken) {

    public PageQuery {
        sort = sort == null ? List.of() : List.copyOf(sort);
        filters = filters == null ? List.of() : List.copyOf(filters);
    }

    public PageQuery(int page, int size, List<SortField> sort, List<FilterCriteria> filters, String search) {
        this(page, size, sort, filters, search, null);
    }

    public Pageable toPageable() {
        if (sort.isEmpty()) {
            return PageRequest.of(page, size);
        }
        List<Sort.Order> orders = new ArrayList<>(sort.size());
        for (SortField sf : sort) {
            orders.add(sf.toOrder());
        }
        return PageRequest.of(page, size, Sort.by(orders));
    }

    public static PageQuery defaults(int defaultSize, String defaultSortField, SortDirection defaultDir) {
        List<SortField> sort = defaultSortField == null || defaultSortField.isBlank()
                ? List.of()
                : List.of(new SortField(defaultSortField, defaultDir == null ? SortDirection.DESC : defaultDir));
        return new PageQuery(0, defaultSize, sort, Collections.emptyList(), null, null);
    }
}
