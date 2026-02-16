package com.ksa.islamic.reporting.projector;

/**
 * Strategy for how to apply an event to the read model.
 */
public enum ProjectionStrategy {

    /**
     * Update the read model incrementally.
     * Most efficient for simple updates to existing records.
     */
    INCREMENTAL,

    /**
     * Rebuild the entire read model from scratch.
     * Used for complex aggregations or when the read model structure changes.
     */
    REBUILD,

    /**
     * Skip this event (e.g., not relevant to this projection).
     */
    SKIP
}