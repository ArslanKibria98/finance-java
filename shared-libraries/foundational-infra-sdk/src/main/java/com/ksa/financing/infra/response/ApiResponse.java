package com.ksa.financing.infra.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ksa.financing.infra.pagination.PageMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard envelope around every controller response. Existing wire format is
 * preserved exactly:
 * <pre>
 * { "data": ..., "message": "...", "timestamp": "..." }
 * </pre>
 *
 * <p>For paginated list endpoints an optional {@code pagination} field is added.
 * It is omitted entirely (via {@code JsonInclude.NON_NULL}) on non-paginated
 * responses so existing clients see no change.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"data", "pagination", "message", "timestamp"})
public class ApiResponse<T> {
    private T data;
    private PageMetadata pagination;
    private String message;
    private Instant timestamp;

    public static <T> ApiResponse<T> of(T data, String message) {
        return ApiResponse.<T>builder()
                .data(data)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> of(T data) {
        return of(data, "success");
    }
}
