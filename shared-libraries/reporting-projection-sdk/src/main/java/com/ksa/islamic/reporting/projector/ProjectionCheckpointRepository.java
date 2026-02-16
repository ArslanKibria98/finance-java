package com.ksa.islamic.reporting.projector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing projection checkpoints.
 */
@Repository
public interface ProjectionCheckpointRepository extends JpaRepository<ProjectionCheckpoint, UUID> {

    /**
     * Find checkpoint by projector name and event ID (for idempotency check).
     */
    Optional<ProjectionCheckpoint> findByProjectorNameAndEventId(String projectorName, String eventId);

    /**
     * Find the latest checkpoint for a projector.
     */
    Optional<ProjectionCheckpoint> findTopByProjectorNameOrderByOffsetDesc(String projectorName);

    /**
     * Find all checkpoints for a projector within a time range.
     */
    @Query("SELECT c FROM ProjectionCheckpoint c WHERE c.projectorName = :projectorName " +
            "AND c.eventTimestamp BETWEEN :startTime AND :endTime ORDER BY c.eventTimestamp")
    List<ProjectionCheckpoint> findByProjectorAndTimeRange(
            @Param("projectorName") String projectorName,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Delete old checkpoints to prevent table growth.
     */
    @Modifying
    @Query("DELETE FROM ProjectionCheckpoint c WHERE c.processedAt < :cutoffTime")
    int deleteOldCheckpoints(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Get processing statistics for a projector.
     */
    @Query("SELECT COUNT(c), MIN(c.eventTimestamp), MAX(c.eventTimestamp), AVG(EXTRACT(EPOCH FROM (c.processedAt - c.eventTimestamp))) " +
            "FROM ProjectionCheckpoint c WHERE c.projectorName = :projectorName")
    Object[] getProjectorStatistics(@Param("projectorName") String projectorName);

    /**
     * Find checkpoints with high processing lag.
     */
    @Query("SELECT c FROM ProjectionCheckpoint c WHERE " +
            "EXTRACT(EPOCH FROM (c.processedAt - c.eventTimestamp)) > :maxLagSeconds " +
            "ORDER BY c.processedAt DESC")
    List<ProjectionCheckpoint> findHighLagCheckpoints(@Param("maxLagSeconds") double maxLagSeconds);

    /**
     * Count events processed by each projector.
     */
    @Query("SELECT c.projectorName, COUNT(c) FROM ProjectionCheckpoint c " +
            "GROUP BY c.projectorName")
    List<Object[]> countByProjector();
}