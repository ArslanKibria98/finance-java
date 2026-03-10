package com.ksa.financing.infra.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final List<Locale> SUPPORTED_LOCALES = List.of(
            Locale.ENGLISH,
            Locale.forLanguageTag("en-US"),
            Locale.forLanguageTag("ar"),
            Locale.forLanguageTag("ar-SA")
    );
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private final MessageSource errorMessageSource;

    public GlobalExceptionHandler(MessageSource errorMessageSource) {
        this.errorMessageSource = errorMessageSource;
    }

    // ── Custom Exception Handlers ───────────────────────────────────

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("Business error: {} - {}", ex.getErrorCode(), ex.getMessage());

        Locale locale = resolveLocale(request);
        String localizedMessage = resolveMessage(ex.getErrorCode(), ex.getArgs(), ex.getMessage(), locale);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase())
                .code(ex.getErrorCode())
                .message(localizedMessage)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ErrorResponse> handleTechnicalException(TechnicalException ex, WebRequest request) {
        log.error("Technical error: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);

        Locale locale = resolveLocale(request);
        String localizedMessage = resolveMessage(
                ErrorCodes.TECHNICAL_ERROR, ex.getArgs(),
                "A system error occurred. Please try again later.", locale);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .code(ex.getErrorCode())
                .message(localizedMessage)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex, WebRequest request) {
        log.debug("Resource not found: {}", ex.getMessage());

        Locale locale = resolveLocale(request);
        String localizedMessage = resolveMessage(ex.getErrorCode(), ex.getArgs(), ex.getMessage(), locale);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .code(ex.getErrorCode())
                .message(localizedMessage)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // ── Spring Security Exception Handlers ──────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());

        Locale locale = resolveLocale(request);
        String localizedMessage = resolveMessage(
                ErrorCodes.ACCESS_DENIED, null,
                "You do not have permission to access this resource", locale);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .code(ErrorCodes.ACCESS_DENIED)
                .message(localizedMessage)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        log.warn("Bad credentials: {}", ex.getMessage());

        Locale locale = resolveLocale(request);
        String localizedMessage = resolveMessage(
                ErrorCodes.INVALID_CREDENTIALS, null,
                "Invalid username or password", locale);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .code(ErrorCodes.INVALID_CREDENTIALS)
                .message(localizedMessage)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // ── Validation Exception Handler ────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        Locale locale = resolveLocale(request);
        String localizedMessage = resolveMessage(
                ErrorCodes.VALIDATION_FAILED, null,
                "Validation failed", locale);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .code(ErrorCodes.VALIDATION_FAILED)
                .message(localizedMessage)
                .path(extractPath(request))
                .details(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ── JDK Exception Handlers ──────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Bad request: {}", ex.getMessage());

        Locale locale = resolveLocale(request);
        String localizedMessage = resolveMessage(
                ErrorCodes.BAD_REQUEST, new Object[]{ex.getMessage()},
                ex.getMessage(), locale);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .code(ErrorCodes.BAD_REQUEST)
                .message(localizedMessage)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        log.warn("Conflict: {}", ex.getMessage());

        Locale locale = resolveLocale(request);
        String localizedMessage = resolveMessage(
                ErrorCodes.CONFLICT, null,
                ex.getMessage(), locale);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .code(ErrorCodes.CONFLICT)
                .message(localizedMessage)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ── Catch-All Handler ───────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);

        Locale locale = resolveLocale(request);
        String localizedMessage = resolveMessage(
                ErrorCodes.INTERNAL_ERROR, null,
                "An unexpected error occurred", locale);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .code(ErrorCodes.INTERNAL_ERROR)
                .message(localizedMessage)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // ── Private Utility Methods ─────────────────────────────────────

    /**
     * Resolves a localized message using the error code as the message key.
     * Falls back to the provided fallback message if the key is not found.
     */
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

    /**
     * Resolves the locale from the Accept-Language header.
     * Defaults to English for backward compatibility.
     */
    private Locale resolveLocale(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            String acceptLanguage = servletRequest.getHeader("Accept-Language");
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
        }
        return DEFAULT_LOCALE;
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
