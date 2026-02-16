package com.ksa.islamic.reporting.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration and management for materialized views.
 * Handles scheduled refreshes and monitoring.
 */
@Slf4j
@Service
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class MaterializedViewConfig {

    private final ProjectionConfig projectionConfig;
    private final JdbcTemplate jdbcTemplate;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    /**
     * Materialized view definitions.
     */
    public enum MaterializedView {
        LOAN_PORTFOLIO("loan_portfolio_view"),
        OVERDUE_LOANS("overdue_loans_view"),
        REVENUE("revenue_view"),
        CUSTOMER_SEGMENTATION("customer_segmentation_view"),
        PRODUCT_PERFORMANCE("product_performance_view"),
        COLLECTION_EFFICIENCY("collection_efficiency_view");

        private final String viewName;

        MaterializedView(String viewName) {
            this.viewName = viewName;
        }

        public String getViewName() {
            return viewName;
        }
    }

    /**
     * Refresh loan portfolio view - runs every 15 minutes.
     */
    @Scheduled(cron = "#{projectionConfig.materializedViews.schedules.portfolio}")
    public void refreshLoanPortfolioView() {
        refreshView(MaterializedView.LOAN_PORTFOLIO);
    }

    /**
     * Refresh overdue loans view - runs every 10 minutes.
     */
    @Scheduled(cron = "#{projectionConfig.materializedViews.schedules.overdue}")
    public void refreshOverdueLoansView() {
        refreshView(MaterializedView.OVERDUE_LOANS);
    }

    /**
     * Refresh revenue view - runs every hour.
     */
    @Scheduled(cron = "#{projectionConfig.materializedViews.schedules.revenue}")
    public void refreshRevenueView() {
        refreshView(MaterializedView.REVENUE);
    }

    /**
     * Refresh customer segmentation view - runs every 4 hours.
     */
    @Scheduled(cron = "#{projectionConfig.materializedViews.schedules.segmentation}")
    public void refreshCustomerSegmentationView() {
        refreshView(MaterializedView.CUSTOMER_SEGMENTATION);
    }

    /**
     * Refresh product performance view - runs every 2 hours.
     */
    @Scheduled(cron = "#{projectionConfig.materializedViews.schedules.productPerformance}")
    public void refreshProductPerformanceView() {
        refreshView(MaterializedView.PRODUCT_PERFORMANCE);
    }

    /**
     * Refresh collection efficiency view - runs every hour at 30 minutes.
     */
    @Scheduled(cron = "#{projectionConfig.materializedViews.schedules.collectionEfficiency}")
    public void refreshCollectionEfficiencyView() {
        refreshView(MaterializedView.COLLECTION_EFFICIENCY);
    }

    /**
     * Refresh a specific materialized view.
     */
    public CompletableFuture<ViewRefreshResult> refreshView(MaterializedView view) {
        if (!projectionConfig.getMaterializedViews().isAutoRefresh()) {
            log.debug("Auto-refresh is disabled for materialized views");
            return CompletableFuture.completedFuture(ViewRefreshResult.skipped(view));
        }

        return CompletableFuture.supplyAsync(() -> {
            Instant startTime = Instant.now();
            log.info("Starting refresh of materialized view: {}", view.getViewName());

            try {
                String refreshCommand = buildRefreshCommand(view);
                jdbcTemplate.execute(refreshCommand);

                Duration duration = Duration.between(startTime, Instant.now());
                log.info("Successfully refreshed view {} in {}ms",
                        view.getViewName(), duration.toMillis());

                return ViewRefreshResult.success(view, duration);

            } catch (Exception e) {
                Duration duration = Duration.between(startTime, Instant.now());
                log.error("Failed to refresh materialized view: {}", view.getViewName(), e);
                return ViewRefreshResult.failure(view, duration, e);
            }
        }, executorService);
    }

    /**
     * Build the refresh command based on configuration.
     */
    private String buildRefreshCommand(MaterializedView view) {
        StringBuilder command = new StringBuilder("REFRESH MATERIALIZED VIEW ");

        if (projectionConfig.getMaterializedViews().isConcurrentRefresh()) {
            command.append("CONCURRENTLY ");
        }

        command.append(view.getViewName());
        return command.toString();
    }

    /**
     * Refresh all materialized views.
     */
    public CompletableFuture<Map<MaterializedView, ViewRefreshResult>> refreshAllViews() {
        Map<MaterializedView, CompletableFuture<ViewRefreshResult>> futures = new HashMap<>();

        for (MaterializedView view : MaterializedView.values()) {
            futures.put(view, refreshView(view));
        }

        return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Map<MaterializedView, ViewRefreshResult> results = new HashMap<>();
                    futures.forEach((view, future) -> {
                        results.put(view, future.join());
                    });
                    return results;
                });
    }

    /**
     * Get statistics about materialized view sizes and last refresh times.
     */
    public Map<String, ViewStatistics> getViewStatistics() {
        Map<String, ViewStatistics> statistics = new HashMap<>();

        String query = """
            SELECT
                schemaname,
                matviewname,
                pg_size_pretty(pg_total_relation_size(schemaname||'.'||matviewname)) as size,
                pg_stat_get_last_analyze_time(c.oid) as last_analyze,
                pg_stat_get_last_autoanalyze_time(c.oid) as last_autoanalyze
            FROM pg_matviews
            JOIN pg_class c ON c.relname = matviewname
            WHERE schemaname = 'public'
        """;

        jdbcTemplate.query(query, rs -> {
            ViewStatistics stats = new ViewStatistics();
            stats.setViewName(rs.getString("matviewname"));
            stats.setSize(rs.getString("size"));
            stats.setLastAnalyze(rs.getTimestamp("last_analyze"));
            stats.setLastAutoAnalyze(rs.getTimestamp("last_autoanalyze"));
            statistics.put(stats.getViewName(), stats);
        });

        return statistics;
    }

    /**
     * Result of a view refresh operation.
     */
    @lombok.Value
    @lombok.Builder
    public static class ViewRefreshResult {
        MaterializedView view;
        boolean success;
        Duration duration;
        Exception error;
        Instant timestamp;

        public static ViewRefreshResult success(MaterializedView view, Duration duration) {
            return ViewRefreshResult.builder()
                    .view(view)
                    .success(true)
                    .duration(duration)
                    .timestamp(Instant.now())
                    .build();
        }

        public static ViewRefreshResult failure(MaterializedView view, Duration duration, Exception error) {
            return ViewRefreshResult.builder()
                    .view(view)
                    .success(false)
                    .duration(duration)
                    .error(error)
                    .timestamp(Instant.now())
                    .build();
        }

        public static ViewRefreshResult skipped(MaterializedView view) {
            return ViewRefreshResult.builder()
                    .view(view)
                    .success(true)
                    .duration(Duration.ZERO)
                    .timestamp(Instant.now())
                    .build();
        }
    }

    /**
     * Statistics about a materialized view.
     */
    @lombok.Data
    public static class ViewStatistics {
        private String viewName;
        private String size;
        private java.sql.Timestamp lastAnalyze;
        private java.sql.Timestamp lastAutoAnalyze;
    }
}