package com.ksa.financing.infra.exception;

import com.ksa.financing.infra.security.blacklist.BlacklistViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
        Object[] args = (ex.getArgs() != null && ex.getArgs().length > 0)
                ? ex.getArgs()
                : new Object[]{ex.getMessage()};
        String localizedMessage = resolveMessage(ex.getErrorCode(), args, ex.getMessage(), locale);

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
        Object[] args = (ex.getArgs() != null && ex.getArgs().length > 0)
                ? ex.getArgs()
                : new Object[]{ex.getMessage()};
        String localizedMessage = resolveMessage(
                ErrorCodes.TECHNICAL_ERROR, args,
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
        Object[] args = (ex.getArgs() != null && ex.getArgs().length > 0)
                ? ex.getArgs()
                : new Object[]{ex.getMessage()};
        String localizedMessage = resolveMessage(ex.getErrorCode(), args, ex.getMessage(), locale);

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

    @ExceptionHandler(BlacklistViolationException.class)
    public ResponseEntity<ErrorResponse> handleBlacklistViolation(BlacklistViolationException ex, WebRequest request) {
        log.warn("Blacklist violation: type={}, reason={}", ex.getType(), ex.getReason());

        Locale locale = resolveLocale(request);
        Object[] args = ex.getReason() != null ? new Object[]{ex.getReason()} : new Object[]{"blacklisted"};
        String localizedMessage = resolveMessage(ex.getErrorCode(), args, ex.getMessage(), locale);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .code(ex.getErrorCode())
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
        Locale locale = resolveLocale(request);
        Map<String, String> validationErrors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = (error instanceof FieldError fieldError) ? fieldError.getField() : error.getObjectName();
            String messageKey = error.getDefaultMessage();
            String resolvedMessage = resolveMessage(messageKey, error.getArguments(), messageKey, locale);
            validationErrors.put(fieldName, resolvedMessage);
        });

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

    // ── Spring MVC Exception Handlers ──────────────────────────────

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(MissingServletRequestParameterException ex, WebRequest request) {
        log.warn("Missing request parameter: {}", ex.getParameterName());

        Locale locale = resolveLocale(request);
        String fallback = "Required request parameter '" + ex.getParameterName() + "' is not present";
        String localizedMessage = resolveMessage(
                ErrorCodes.BAD_REQUEST, new Object[]{fallback}, fallback, locale);

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

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        log.warn("HTTP method not supported: {} (supported: {})", ex.getMethod(), ex.getSupportedHttpMethods());

        Locale locale = resolveLocale(request);
        String supported = ex.getSupportedHttpMethods() != null
                ? ex.getSupportedHttpMethods().toString() : "";
        String fallback = "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint."
                + (supported.isBlank() ? "" : " Supported methods: " + supported);
        String localizedMessage = resolveMessage(
                ErrorCodes.BAD_REQUEST, new Object[]{fallback}, fallback, locale);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error(HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase())
                .code(ErrorCodes.BAD_REQUEST)
                .message(localizedMessage)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, WebRequest request) {
        log.warn("Content-Type not supported: {}", ex.getContentType());

        Locale locale = resolveLocale(request);
        String fallback = "Content-Type '" + ex.getContentType() + "' is not supported. Supported: "
                + ex.getSupportedMediaTypes();
        String localizedMessage = resolveMessage(
                ErrorCodes.BAD_REQUEST, new Object[]{fallback}, fallback, locale);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .error(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())
                .code(ErrorCodes.BAD_REQUEST)
                .message(localizedMessage)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.warn("Argument type mismatch: {} = {}", ex.getName(), ex.getValue());

        Locale locale = resolveLocale(request);
        String fallback = "Parameter '" + ex.getName() + "' has an invalid value: " + ex.getValue();
        String localizedMessage = resolveMessage(
                ErrorCodes.BAD_REQUEST, new Object[]{fallback}, fallback, locale);

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

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(Exception ex, WebRequest request) {
        log.debug("Resource not found: {}", ex.getMessage());

        Locale locale = resolveLocale(request);
        String localizedMessage = resolveMessage(
                ErrorCodes.NOT_FOUND, null,
                "The requested resource was not found", locale);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .code(ErrorCodes.NOT_FOUND)
                .message(localizedMessage)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
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
