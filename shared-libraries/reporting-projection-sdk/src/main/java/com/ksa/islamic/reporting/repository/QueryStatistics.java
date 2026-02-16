package com.ksa.islamic.reporting.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

/**
 * Statistics about query performance for monitoring and optimization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryStatistics {

    private long totalQueries;
    private long totalRows;
    private Duration averageQueryTime;
    private Duration maxQueryTime;
    private Duration minQueryTime;
    private Instant lastQueryTime;
    private double cacheHitRate;

    public static QueryStatistics empty() {
        return QueryStatistics.builder()
                .totalQueries(0L)
                .totalRows(0L)
                .averageQueryTime(Duration.ZERO)
                .maxQueryTime(Duration.ZERO)
                .minQueryTime(Duration.ZERO)
                .cacheHitRate(0.0)
                .build();
    }
}