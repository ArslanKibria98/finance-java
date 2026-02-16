package com.ksa.financing.infra.logging;

import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;

public class StructuredLogger {

    private final Logger logger;

    private StructuredLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public static StructuredLogger getLogger(Class<?> clazz) {
        return new StructuredLogger(clazz);
    }

    public void info(String message, Object... keyValues) {
        if (logger.isInfoEnabled()) {
            logger.info(message, StructuredArguments.entries(asMap(keyValues)));
        }
    }

    public void warn(String message, Object... keyValues) {
        if (logger.isWarnEnabled()) {
            logger.warn(message, StructuredArguments.entries(asMap(keyValues)));
        }
    }

    public void error(String message, Throwable throwable, Object... keyValues) {
        if (logger.isErrorEnabled()) {
            logger.error(message, StructuredArguments.entries(asMap(keyValues)), throwable);
        }
    }

    public void debug(String message, Object... keyValues) {
        if (logger.isDebugEnabled()) {
            logger.debug(message, StructuredArguments.entries(asMap(keyValues)));
        }
    }

    public void withContext(Map<String, String> contextData, Runnable action) {
        contextData.forEach(MDC::put);
        try {
            action.run();
        } finally {
            contextData.keySet().forEach(MDC::remove);
        }
    }

    private Map<String, Object> asMap(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value pairs must be even");
        }

        Map<String, Object> map = new java.util.HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i].toString(), keyValues[i + 1]);
        }
        return map;
    }
}
