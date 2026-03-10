package com.ksa.financing.infra.response;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method or class to skip automatic ApiResponse wrapping.
 * Use on health checks, webhooks, file downloads, or any endpoint that
 * needs to return its raw response body.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RawResponse {
}
