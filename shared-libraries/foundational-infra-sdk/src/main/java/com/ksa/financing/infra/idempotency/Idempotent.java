package com.ksa.financing.infra.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * SpEL expression to generate idempotency key
     */
    String keyExpression();

    /**
     * TTL in seconds (default 24 hours)
     */
    long ttlSeconds() default 86400;
}
