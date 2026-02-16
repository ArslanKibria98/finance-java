package com.ksa.islamic.reporting.projector;

/**
 * Exception thrown when a projection fails.
 */
public class ProjectionException extends RuntimeException {

    public ProjectionException(String message) {
        super(message);
    }

    public ProjectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProjectionException(Throwable cause) {
        super(cause);
    }
}