package com.ksa.financing.infra.pagination;

import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Internal carrier returned by use-case/repository layers. The
 * {@code ApiResponseAdvice} unwraps this so the wire format stays:
 * <pre>
 * { "data": [...], "pagination": {...}, "message": "...", "timestamp": "..." }
 * </pre>
 *
 * <p>This type itself is never serialized — controllers may return either
 * {@code PageResponse<T>} or {@code List<T>}; the advice handles both.
 */
public record PageResponse<T>(List<T> content, PageMetadata pagination) {

    public PageResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public static <T> PageResponse<T> from(Page<T> springPage) {
        return new PageResponse<>(springPage.getContent(), PageMetadata.from(springPage));
    }

    public static <E, T> PageResponse<T> from(Page<E> springPage, Function<E, T> mapper) {
        List<T> mapped = springPage.getContent().stream().map(mapper).toList();
        return new PageResponse<>(mapped, PageMetadata.from(springPage));
    }

    public <R> PageResponse<R> map(Function<T, R> mapper) {
        return new PageResponse<>(content.stream().map(mapper).toList(), pagination);
    }

    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(
                Collections.emptyList(),
                new PageMetadata(page, size, 0L, 0, true, true, true));
    }
}
