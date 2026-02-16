package com.ksa.islamic.reporting.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration properties for projection system.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "projection")
public class ProjectionConfig {

    /**
     * Enable or disable projection processing.
     */
    private boolean enabled = true;

    /**
     * Number of threads for parallel projection processing.
     */
    private int processingThreads = 4;

    /**
     * Batch size for processing events.
     */
    private int batchSize = 100;

    /**
     * Maximum retry attempts for failed projections.
     */
    private int maxRetries = 3;

    /**
     * Retry delay between attempts.
     */
    private Duration retryDelay = Duration.ofSeconds(5);

    /**
     * Checkpoint cleanup settings.
     */
    private CheckpointCleanup checkpointCleanup = new CheckpointCleanup();

    /**
     * Materialized view refresh settings.
     */
    private MaterializedViewSettings materializedViews = new MaterializedViewSettings();

    /**
     * Performance monitoring settings.
     */
    private PerformanceSettings performance = new PerformanceSettings();

    @Data
    public static class CheckpointCleanup {
        /**
         * Enable automatic cleanup of old checkpoints.
         */
        private boolean enabled = true;

        /**
         * Retention period for checkpoints.
         */
        private Duration retentionPeriod = Duration.ofDays(30);

        /**
         * Cleanup schedule (cron expression).
         */
        private String schedule = "0 0 2 * * ?"; // Daily at 2 AM
    }

    @Data
    public static class MaterializedViewSettings {
        /**
         * Enable automatic refresh of materialized views.
         */
        private boolean autoRefresh = true;

        /**
         * Refresh mode: INCREMENTAL or FULL.
         */
        private RefreshMode refreshMode = RefreshMode.INCREMENTAL;

        /**
         * Refresh schedule for each view type.
         */
        private ViewSchedules schedules = new ViewSchedules();

        /**
         * Use concurrent refresh (requires unique index).
         */
        private boolean concurrentRefresh = true;
    }

    @Data
    public static class ViewSchedules {
        /**
         * Portfolio view refresh schedule (cron).
         */
        private String portfolio = "0 */15 * * * ?"; // Every 15 minutes

        /**
         * Overdue loans view refresh schedule.
         */
        private String overdue = "0 */10 * * * ?"; // Every 10 minutes

        /**
         * Revenue view refresh schedule.
         */
        private String revenue = "0 0 * * * ?"; // Every hour

        /**
         * Customer segmentation refresh schedule.
         */
        private String segmentation = "0 0 */4 * * ?"; // Every 4 hours

        /**
         * Product performance refresh schedule.
         */
        private String productPerformance = "0 0 */2 * * ?"; // Every 2 hours

        /**
         * Collection efficiency refresh schedule.
         */
        private String collectionEfficiency = "0 30 * * * ?"; // Every hour at 30 minutes
    }

    @Data
    public static class PerformanceSettings {
        /**
         * Enable query performance tracking.
         */
        private boolean trackQueries = true;

        /**
         * Slow query threshold.
         */
        private Duration slowQueryThreshold = Duration.ofMillis(100);

        /**
         * Enable query result caching.
         */
        private boolean enableCache = true;

        /**
         * Cache TTL for read model queries.
         */
        private Duration cacheTtl = Duration.ofMinutes(5);

        /**
         * Maximum cache size.
         */
        private long maxCacheSize = 10000;
    }

    public enum RefreshMode {
        INCREMENTAL,
        FULL
    }
}