package com.ksa.financing.infra.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Custom AuthenticationEntryPoint that returns structured JSON error responses
 * for 401 Unauthorized errors (missing/invalid/expired JWT tokens).
 *
 * <p>Spring Security handles authentication failures at the filter level,
 * before reaching {@code @RestControllerAdvice}. This entry point ensures
 * consistent JSON responses with localization support.</p>
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JsonAuthenticationEntryPoint.class);

    private static final List<Locale> SUPPORTED_LOCALES = List.of(
            Locale.ENGLISH,
            Locale.forLanguageTag("en-US"),
            Locale.forLanguageTag("ar"),
            Locale.forLanguageTag("ar-SA")
    );
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private final ObjectMapper objectMapper;
    private final MessageSource errorMessageSource;

    public JsonAuthenticationEntryPoint(MessageSource errorMessageSource) {
        this.errorMessageSource = errorMessageSource;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.debug("Authentication failed for {}: {}", request.getRequestURI(), authException.getMessage());

        Locale locale = resolveLocale(request);
        String localizedMessage = resolveMessage(
                ErrorCodes.INVALID_CREDENTIALS, null,
                "Authentication is required to access this resource", locale);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .code(ErrorCodes.INVALID_CREDENTIALS)
                .message(localizedMessage)
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    private String resolveMessage(String code, Object[] args, String fallback, Locale locale) {
        if (code == null || errorMessageSource == null) {
            return fallback;
        }
        try {
            return errorMessageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            return fallback;
        }
    }

    private Locale resolveLocale(HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage != null && !acceptLanguage.isBlank()) {
            try {
                List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(acceptLanguage);
                Locale matched = Locale.lookup(ranges, SUPPORTED_LOCALES);
                if (matched != null) {
                    return matched;
                }
            } catch (IllegalArgumentException ignored) {
                // Malformed Accept-Language header, fall through to default
            }
        }
        return DEFAULT_LOCALE;
    }
}
