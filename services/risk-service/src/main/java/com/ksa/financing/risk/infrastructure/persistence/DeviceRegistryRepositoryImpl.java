package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.domain.model.DeviceRegistryEntry;
import com.ksa.financing.risk.domain.port.out.DeviceRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DeviceRegistryRepositoryImpl implements DeviceRegistryRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<DeviceRegistryEntry> findByDeviceId(String deviceId) {
        var results = jdbcTemplate.query(
            """
            SELECT dr.*, nc.nid_count
            FROM device_registry dr
            JOIN (
                SELECT device_id, COUNT(DISTINCT nid_hash) AS nid_count
                FROM device_registry GROUP BY device_id
            ) nc ON dr.device_id = nc.device_id
            WHERE dr.device_id = ?
            ORDER BY dr.last_seen_at DESC LIMIT 1
            """,
            DEVICE_WITH_NID_COUNT_MAPPER, deviceId
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public List<DeviceRegistryEntry> findAllBlocked() {
        return jdbcTemplate.query(
            """
            SELECT dr.*,
                   nid_counts.nid_count,
                   CASE
                       WHEN dr.is_blocked = true THEN 'ADMIN_BLOCKED'
                       WHEN nid_counts.nid_count > 3 AND NOT dr.nid_farming_override THEN 'IDENTITY_FARMING'
                       WHEN dr.attempt_count > 5 THEN 'VELOCITY_EXCEEDED'
                   END AS block_source
            FROM device_registry dr
            INNER JOIN (
                SELECT device_id, COUNT(DISTINCT nid_hash) AS nid_count
                FROM device_registry GROUP BY device_id
            ) nid_counts ON dr.device_id = nid_counts.device_id
            WHERE dr.device_id IN (
                SELECT DISTINCT d.device_id FROM device_registry d
                LEFT JOIN (
                    SELECT device_id, COUNT(DISTINCT nid_hash) AS cnt
                    FROM device_registry GROUP BY device_id
                ) nc ON d.device_id = nc.device_id
                WHERE d.is_blocked = true
                   OR (nc.cnt > 3 AND NOT d.nid_farming_override)
                   OR d.attempt_count > 5
            )
            ORDER BY dr.device_id, dr.last_seen_at DESC
            """,
            BLOCKED_DEVICE_MAPPER
        );
    }

    @Override
    public List<DeviceRegistryEntry> findAll() {
        return jdbcTemplate.query(
            "SELECT * FROM device_registry ORDER BY last_seen_at DESC",
            DEVICE_MAPPER
        );
    }

    @Override
    public void updateBlockStatus(String deviceId, boolean blocked, String reason) {
        jdbcTemplate.update(
            "UPDATE device_registry SET is_blocked = ?, block_reason = ?, updated_at = ? WHERE device_id = ?",
            blocked, reason, Timestamp.from(Instant.now()), deviceId
        );
    }

    @Override
    public void resetAttempts(String deviceId) {
        // Reset attempt_count AND last_seen_at so the 24h velocity window no longer counts these rows
        jdbcTemplate.update(
            "UPDATE device_registry SET attempt_count = 0, last_seen_at = NOW() - INTERVAL '25 HOURS', updated_at = ? WHERE device_id = ?",
            Timestamp.from(Instant.now()), deviceId
        );
    }

    @Override
    public boolean hasAnyRowWithExceededAttempts(String deviceId, int threshold) {
        var count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM device_registry WHERE device_id = ? AND attempt_count > ?",
            Integer.class, deviceId, threshold
        );
        return count != null && count > 0;
    }

    @Override
    public void resetFarmingOverride(String deviceId) {
        jdbcTemplate.update(
            "UPDATE device_registry SET nid_farming_override = TRUE, updated_at = ? WHERE device_id = ?",
            Timestamp.from(Instant.now()), deviceId
        );
    }

    @Override
    public void deleteByDeviceId(String deviceId) {
        jdbcTemplate.update(
            "DELETE FROM device_registry WHERE device_id = ?", deviceId
        );
    }

    @Override
    public boolean existsByDeviceId(String deviceId) {
        var count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM device_registry WHERE device_id = ?",
            Integer.class, deviceId
        );
        return count != null && count > 0;
    }

    private static final RowMapper<DeviceRegistryEntry> DEVICE_WITH_NID_COUNT_MAPPER = (rs, rowNum) -> new DeviceRegistryEntry(
        rs.getObject("id", UUID.class),
        rs.getString("device_id"),
        rs.getString("device_fingerprint"),
        rs.getString("nid_hash"),
        rs.getBoolean("is_blocked"),
        rs.getString("block_reason"),
        rs.getBoolean("is_blocked") ? "ADMIN_BLOCKED" : null,
        rs.getInt("nid_count"),
        rs.getInt("attempt_count"),
        toInstant(rs.getTimestamp("first_seen_at")),
        toInstant(rs.getTimestamp("last_seen_at")),
        toInstant(rs.getTimestamp("created_at")),
        toInstant(rs.getTimestamp("updated_at"))
    );

    private static final RowMapper<DeviceRegistryEntry> DEVICE_MAPPER = (rs, rowNum) -> new DeviceRegistryEntry(
        rs.getObject("id", UUID.class),
        rs.getString("device_id"),
        rs.getString("device_fingerprint"),
        rs.getString("nid_hash"),
        rs.getBoolean("is_blocked"),
        rs.getString("block_reason"),
        rs.getBoolean("is_blocked") ? "ADMIN_BLOCKED" : null,
        0,
        rs.getInt("attempt_count"),
        toInstant(rs.getTimestamp("first_seen_at")),
        toInstant(rs.getTimestamp("last_seen_at")),
        toInstant(rs.getTimestamp("created_at")),
        toInstant(rs.getTimestamp("updated_at"))
    );

    private static final RowMapper<DeviceRegistryEntry> BLOCKED_DEVICE_MAPPER = (rs, rowNum) -> new DeviceRegistryEntry(
        rs.getObject("id", UUID.class),
        rs.getString("device_id"),
        rs.getString("device_fingerprint"),
        rs.getString("nid_hash"),
        rs.getBoolean("is_blocked"),
        rs.getString("block_reason"),
        rs.getString("block_source"),
        rs.getInt("nid_count"),
        rs.getInt("attempt_count"),
        toInstant(rs.getTimestamp("first_seen_at")),
        toInstant(rs.getTimestamp("last_seen_at")),
        toInstant(rs.getTimestamp("created_at")),
        toInstant(rs.getTimestamp("updated_at"))
    );

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
