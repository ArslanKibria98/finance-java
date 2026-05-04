package com.ksa.financing.infra.pagination;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable defaults for list-endpoint pagination. Override per-service via
 * {@code application.yml}:
 *
 * <pre>
 * ksa:
 *   pagination:
 *     default-page-size: 10
 *     max-page-size: 100
 *     default-sort-field: createdAt
 *     default-sort-direction: DESC
 *     page-param: page
 *     size-param: size
 *     sort-param: sort
 *     filter-param: filter
 *     search-param: search
 * </pre>
 */
@ConfigurationProperties("ksa.pagination")
public class PaginationProperties {

    private int defaultPageSize = 10;
    private int maxPageSize = 100;
    private String defaultSortField = "createdAt";
    private SortDirection defaultSortDirection = SortDirection.DESC;
    private String pageParam = "page";
    private String sizeParam = "size";
    private String sortParam = "sort";
    private String filterParam = "filter";
    private String searchParam = "search";

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public String getDefaultSortField() {
        return defaultSortField;
    }

    public void setDefaultSortField(String defaultSortField) {
        this.defaultSortField = defaultSortField;
    }

    public SortDirection getDefaultSortDirection() {
        return defaultSortDirection;
    }

    public void setDefaultSortDirection(SortDirection defaultSortDirection) {
        this.defaultSortDirection = defaultSortDirection;
    }

    public String getPageParam() {
        return pageParam;
    }

    public void setPageParam(String pageParam) {
        this.pageParam = pageParam;
    }

    public String getSizeParam() {
        return sizeParam;
    }

    public void setSizeParam(String sizeParam) {
        this.sizeParam = sizeParam;
    }

    public String getSortParam() {
        return sortParam;
    }

    public void setSortParam(String sortParam) {
        this.sortParam = sortParam;
    }

    public String getFilterParam() {
        return filterParam;
    }

    public void setFilterParam(String filterParam) {
        this.filterParam = filterParam;
    }

    public String getSearchParam() {
        return searchParam;
    }

    public void setSearchParam(String searchParam) {
        this.searchParam = searchParam;
    }
}
