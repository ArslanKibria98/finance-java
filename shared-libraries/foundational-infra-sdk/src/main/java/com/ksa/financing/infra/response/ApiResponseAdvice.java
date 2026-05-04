package com.ksa.financing.infra.response;

import com.ksa.financing.infra.exception.ErrorResponse;
import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Automatically wraps all controller responses in {@link ApiResponse}.
 *
 * <p>The wrapper {@code message} is driven by the body's {@code failureReason} field.
 * If the field value looks like an error code (e.g. {@code KYC.OTP.INVALID}), it is
 * resolved via {@link MessageSource} to a localized human-readable message respecting
 * the {@code Accept-Language} header. If no localization is found the raw value is
 * returned as-is. When {@code failureReason} is absent/null the message is
 * {@code "success"}.</p>
 */
@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final List<Locale> SUPPORTED_LOCALES = List.of(
            Locale.ENGLISH,
            Locale.forLanguageTag("en-US"),
            Locale.forLanguageTag("ar"),
            Locale.forLanguageTag("ar-SA")
    );
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private final MessageSource errorMessageSource;

    public ApiResponseAdvice(@Qualifier("errorMessageSource") MessageSource errorMessageSource) {
        this.errorMessageSource = errorMessageSource;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (returnType.hasMethodAnnotation(RawResponse.class)) {
            return false;
        }
        if (returnType.getContainingClass().isAnnotationPresent(RawResponse.class)) {
            return false;
        }
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        String path = request.getURI().getPath();

        if (path.startsWith("/actuator") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger")) {
            return body;
        }

        if (body instanceof ApiResponse<?> || body instanceof ErrorResponse) {
            return body;
        }
        if (body instanceof String || body instanceof byte[]) {
            return body;
        }

        // Paginated payload: unwrap so wire format becomes
        //   { data: [...], pagination: {...}, message: ..., timestamp: ... }
        // — existing keys preserved, "data" remains a flat array.
        Object payload = body;
        PageMetadata pagination = null;
        if (body instanceof PageResponse<?> pageResponse) {
            payload = pageResponse.content();
            pagination = pageResponse.pagination();
        }

        String failureReason = extractFailureReason(payload);
        String message;

        if (failureReason != null && !failureReason.isBlank()) {
            Locale locale = resolveLocale(request);
            message = resolveMessage(failureReason, locale);
        } else {
            message = "success";
        }

        return ApiResponse.builder()
                .data(payload)
                .pagination(pagination)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    private String resolveMessage(String code, Locale locale) {
        if (errorMessageSource == null) {
            return code;
        }
        try {
            return errorMessageSource.getMessage(code, null, locale);
        } catch (NoSuchMessageException e) {
            // Not an error code — return the raw string as-is
            return code;
        }
    }

    private Locale resolveLocale(ServerHttpRequest request) {
        List<String> acceptLanguage = request.getHeaders().get("Accept-Language");
        if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
            String header = acceptLanguage.get(0);
            if (header != null && !header.isBlank()) {
                try {
                    List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(header);
                    Locale matched = Locale.lookup(ranges, SUPPORTED_LOCALES);
                    if (matched != null) {
                        return matched;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return DEFAULT_LOCALE;
    }

    private String extractFailureReason(Object body) {
        if (body == null) return null;

        // Record accessor: failureReason()
        try {
            var method = body.getClass().getMethod("failureReason");
            Object value = method.invoke(body);
            if (value instanceof String s) return s;
        } catch (Exception ignored) {}

        // Lombok/POJO getter: getFailureReason()
        try {
            var method = body.getClass().getMethod("getFailureReason");
            Object value = method.invoke(body);
            if (value instanceof String s) return s;
        } catch (Exception ignored) {}

        return null;
    }
}
