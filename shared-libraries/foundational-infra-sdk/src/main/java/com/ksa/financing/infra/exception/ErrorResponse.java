package com.ksa.financing.infra.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private String code;
    private String message;
    private String path;
    private String traceId;
    private Map<String, String> details;

    public static class ErrorResponseBuilder {
        public ErrorResponse build() {
            this.traceId = MDC.get("traceId");
            return new ErrorResponse(timestamp, status, error, code, message, path, traceId, details);
        }
    }
}
