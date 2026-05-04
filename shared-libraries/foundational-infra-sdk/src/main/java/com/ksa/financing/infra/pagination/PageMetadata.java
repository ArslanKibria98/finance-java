package com.ksa.financing.infra.pagination;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;

/**
 * Metadata block surfaced under the {@code pagination} key in API responses.
 * Field names are stable wire-format and must not change without a versioned API change.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageMetadata(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty,
        String nextPageToken) {

    public PageMetadata(int page, int size, long totalElements, int totalPages, boolean first, boolean last, boolean empty) {
        this(page, size, totalElements, totalPages, first, last, empty, null);
    }

    public static PageMetadata from(Page<?> springPage) {
        return new PageMetadata(
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isFirst(),
                springPage.isLast(),
                springPage.isEmpty(),
                null);
    }
}
