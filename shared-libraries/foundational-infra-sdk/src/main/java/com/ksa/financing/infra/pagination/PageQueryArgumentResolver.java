package com.ksa.financing.infra.pagination;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-resolves a {@link PageQuery} method parameter from the HTTP query string.
 *
 * <p>Supported parameters (names configurable via {@link PaginationProperties}):
 * <ul>
 *   <li>{@code page} — zero-based page index (default 0)</li>
 *   <li>{@code size} — page size (default {@link PaginationProperties#getDefaultPageSize()},
 *       capped at {@link PaginationProperties#getMaxPageSize()})</li>
 *   <li>{@code sort} — repeatable; format {@code field,asc|desc}</li>
 *   <li>{@code filter} — repeatable; format {@code field:op:value[,value...]}</li>
 *   <li>{@code search} — global free-text search keyword</li>
 * </ul>
 */
public class PageQueryArgumentResolver implements HandlerMethodArgumentResolver {

    private final PaginationProperties props;

    public PageQueryArgumentResolver(PaginationProperties props) {
        this.props = props;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return PageQuery.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest req = webRequest.getNativeRequest(HttpServletRequest.class);
        if (req == null) {
            return PageQuery.defaults(props.getDefaultPageSize(),
                    props.getDefaultSortField(), props.getDefaultSortDirection());
        }

        int page = parseIntOrDefault(req.getParameter(props.getPageParam()), 0, 0, Integer.MAX_VALUE);
        int size = parseIntOrDefault(req.getParameter(props.getSizeParam()),
                props.getDefaultPageSize(), 1, props.getMaxPageSize());

        List<SortField> sort = new ArrayList<>();
        String[] rawSort = req.getParameterValues(props.getSortParam());
        if (rawSort != null) {
            for (String s : rawSort) {
                SortField sf = SortField.parse(s);
                if (sf != null) sort.add(sf);
            }
        }
        if (sort.isEmpty() && props.getDefaultSortField() != null && !props.getDefaultSortField().isBlank()) {
            sort.add(new SortField(props.getDefaultSortField(), props.getDefaultSortDirection()));
        }

        List<FilterCriteria> filters = new ArrayList<>();
        String[] rawFilters = req.getParameterValues(props.getFilterParam());
        if (rawFilters != null) {
            for (String f : rawFilters) {
                FilterCriteria fc = FilterCriteria.parse(f);
                if (fc != null) filters.add(fc);
            }
        }

        String search = req.getParameter(props.getSearchParam());
        if (search != null) {
            search = search.trim();
            if (search.isEmpty()) search = null;
        }

        return new PageQuery(page, size, sort, filters, search);
    }

    private int parseIntOrDefault(String raw, int defaultVal, int min, int max) {
        if (raw == null || raw.isBlank()) return clamp(defaultVal, min, max);
        try {
            return clamp(Integer.parseInt(raw.trim()), min, max);
        } catch (NumberFormatException ex) {
            return clamp(defaultVal, min, max);
        }
    }

    private int clamp(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
