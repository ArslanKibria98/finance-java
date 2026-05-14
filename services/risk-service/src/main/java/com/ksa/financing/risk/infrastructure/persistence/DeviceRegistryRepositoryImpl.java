package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
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

    /**
     * Common SQL fragment that resolves block_source, block_code, and block_code_id
     * by joining internal_check_configs and block_codes tables.
     *
     * Priority:
     * 1. Admin blocked → uses dr.block_code_id (set by admin) or dr.block_code string
     * 2. Identity Farming → resolves from DEVICE_FINGERPRINT config's block_code_id
     * 3. Velocity Exceeded → resolves from VELOCITY_CHECK config's block_code_id
     */
    private static final String BLOCK_RESOLUTION_COLUMNS = """
                   CASE
                       WHEN dr.is_blocked = true THEN 'Admin Block: ' || COALESCE(dr.block_reason, 'No reason provided')
                       WHEN nc.nid_count > 3 AND NOT dr.nid_farming_override THEN 'Identity Farming: ' || nc.nid_count || ' NIDs associated'
                       WHEN dr.attempt_count > 5 THEN 'Velocity Exceeded: ' || dr.attempt_count || ' attempts in 24h'
                   END AS block_source,
                   CASE
                       WHEN dr.is_blocked = true THEN COALESCE(bc_admin.code, dr.block_code, 'MANUAL_BLOCK')
                       WHEN nc.nid_count > 3 AND NOT dr.nid_farming_override THEN COALESCE(bc_farming.code, 'FRAUD002')
                       WHEN dr.attempt_count > 5 THEN COALESCE(bc_velocity.code, 'VEL001')
                   END AS block_code,
                   CASE
                       WHEN dr.is_blocked = true THEN dr.block_code_id
                       WHEN nc.nid_count > 3 AND NOT dr.nid_farming_override THEN icc_farming.block_code_id
                       WHEN dr.attempt_count > 5 THEN icc_velocity.block_code_id
                   END AS resolved_block_code_id
            """;

    /**
     * Common LEFT JOINs for resolving block codes from internal_check_configs and block_codes tables.
     */
    private static final String BLOCK_RESOLUTION_JOINS = """
            LEFT JOIN internal_check_configs icc_farming ON icc_farming.check_name = 'DEVICE_FINGERPRINT'
            LEFT JOIN block_codes bc_farming ON bc_farming.id = icc_farming.block_code_id
            LEFT JOIN internal_check_configs icc_velocity ON icc_velocity.check_name = 'VELOCITY_CHECK'
            LEFT JOIN block_codes bc_velocity ON bc_velocity.id = icc_velocity.block_code_id
            LEFT JOIN block_codes bc_admin ON bc_admin.id = dr.block_code_id
            """;

    @Override
    public Optional<DeviceRegistryEntry> findByDeviceId(String deviceId) {
        var results = jdbcTemplate.query(
            """
            SELECT dr.*, nc.nid_count,
            """ + BLOCK_RESOLUTION_COLUMNS + """
            FROM device_registry dr
            JOIN (
                SELECT device_id, COUNT(DISTINCT nid_hash) AS nid_count
                FROM device_registry GROUP BY device_id
            ) nc ON dr.device_id = nc.device_id
            """ + BLOCK_RESOLUTION_JOINS + """
            WHERE dr.device_id = ?
            ORDER BY dr.last_seen_at DESC LIMIT 1
            """,
            DEVICE_WITH_NID_COUNT_MAPPER, deviceId
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public List<DeviceRegistryEntry> findAllBlocked(String search) {
        String pat = toLikePattern(search);
        String searchFilter = pat == null ? "" : " AND (LOWER(dr.device_id) LIKE ? OR LOWER(dr.nid) LIKE ? OR LOWER(dr.mobile_number) LIKE ? OR LOWER(dr.block_reason) LIKE ?)";
        Object[] args = pat == null ? new Object[0] : new Object[]{pat, pat, pat, pat};

        return jdbcTemplate.query(
            """
            SELECT dr.*,
                   nid_counts.nid_count,
            """ + BLOCK_RESOLUTION_COLUMNS.replace("nc.", "nid_counts.") + """
            FROM device_registry dr
            INNER JOIN (
                SELECT device_id, COUNT(DISTINCT nid_hash) AS nid_count
                FROM device_registry GROUP BY device_id
            ) nid_counts ON dr.device_id = nid_counts.device_id
            """ + BLOCK_RESOLUTION_JOINS.replace("nc.", "nid_counts.") + """
            WHERE (dr.is_blocked = true
               OR (nid_counts.nid_count > 3 AND NOT dr.nid_farming_override)
               OR dr.attempt_count > 5)
            """ + searchFilter + """
            ORDER BY dr.device_id, dr.last_seen_at DESC
            """,
            BLOCKED_DEVICE_MAPPER, args
        );
    }

    @Override
    public List<DeviceRegistryEntry> findAllForGrouping(String search) {
        String pat = toLikePattern(search);
        String where = pat == null ? "" : " WHERE LOWER(dr.device_id) LIKE ? OR LOWER(dr.nid) LIKE ? OR LOWER(dr.mobile_number) LIKE ? OR LOWER(dr.block_reason) LIKE ?";
        Object[] args = pat == null ? new Object[0] : new Object[]{pat, pat, pat, pat};

        return jdbcTemplate.query(
            """
            SELECT dr.*,
                   nid_counts.nid_count,
            """ + BLOCK_RESOLUTION_COLUMNS.replace("nc.", "nid_counts.") + """
            FROM device_registry dr
            INNER JOIN (
                SELECT device_id, COUNT(DISTINCT nid_hash) AS nid_count
                FROM device_registry GROUP BY device_id
            ) nid_counts ON dr.device_id = nid_counts.device_id
            """ + BLOCK_RESOLUTION_JOINS.replace("nc.", "nid_counts.") + where + """
            ORDER BY dr.device_id, dr.last_seen_at DESC
            """,
            DEVICE_WITH_NID_COUNT_MAPPER, args
        );
    }

    @Override
    public PageResponse<DeviceRegistryEntry> findAll(PageQuery query) {
        String pat = toLikePattern(query.search());
        String where = pat == null ? ""
                : " WHERE LOWER(device_id) LIKE ? OR LOWER(device_fingerprint) LIKE ? OR LOWER(nid) LIKE ? OR LOWER(nid_hash) LIKE ? OR LOWER(mobile_number) LIKE ? OR LOWER(block_reason) LIKE ?";

        long totalElements = Optional.ofNullable(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM device_registry" + where, Long.class,
                pat == null ? new Object[0] : new Object[]{pat, pat, pat, pat, pat, pat})
        ).orElse(0L);

        Object[] qArgs = pat == null
                ? new Object[]{query.size(), query.page() * query.size()}
                : new Object[]{pat, pat, pat, pat, pat, pat, query.size(), query.page() * query.size()};
        List<DeviceRegistryEntry> content = jdbcTemplate.query(
            """
            SELECT dr.*, nc.nid_count,
            """ + BLOCK_RESOLUTION_COLUMNS + """
            FROM device_registry dr
            JOIN (
                SELECT device_id, COUNT(DISTINCT nid_hash) AS nid_count
                FROM device_registry GROUP BY device_id
            ) nc ON dr.device_id = nc.device_id
            """ + BLOCK_RESOLUTION_JOINS + where + " ORDER BY dr.last_seen_at DESC LIMIT ? OFFSET ?",
            DEVICE_WITH_NID_COUNT_MAPPER, qArgs
        );

        return new PageResponse<>(content, buildMetadata(query, totalElements));
    }

    private static String toLikePattern(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return "%" + raw.trim().toLowerCase() + "%";
    }

    private PageMetadata buildMetadata(PageQuery query, long totalElements) {
        int totalPages = query.size() == 0 ? 0 : (int) Math.ceil((double) totalElements / query.size());
        return new PageMetadata(
            query.page(),
            query.size(),
            totalElements,
            totalPages,
            query.page() == 0,
            totalPages == 0 || query.page() >= totalPages - 1,
            totalElements == 0
        );
    }

    @Override
    public void updateBlockStatus(String deviceId, boolean blocked, String reason, UUID blockCodeId) {
        jdbcTemplate.update(
            "UPDATE device_registry SET is_blocked = ?, block_reason = ?, block_code_id = ?, updated_at = ? WHERE device_id = ?",
            blocked, reason, blockCodeId, Timestamp.from(Instant.now()), deviceId
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

    // Row mapper that reads resolved_block_code_id instead of raw block_code_id
    private static final RowMapper<DeviceRegistryEntry> DEVICE_WITH_NID_COUNT_MAPPER = (rs, rowNum) -> new DeviceRegistryEntry(
        rs.getObject("id", UUID.class),
        rs.getString("device_id"),
        rs.getString("device_fingerprint"),
        rs.getString("nid"),
        rs.getString("mobile_number"),
        rs.getBoolean("is_blocked"),
        rs.getString("block_reason"),
        rs.getString("block_source"),
        rs.getString("block_code"),
        rs.getObject("resolved_block_code_id", UUID.class),
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
        rs.getString("nid"),
        rs.getString("mobile_number"),
        rs.getBoolean("is_blocked"),
        rs.getString("block_reason"),
        rs.getString("block_source"),
        rs.getString("block_code"),
        rs.getObject("resolved_block_code_id", UUID.class),
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
        rs.getString("nid"),
        rs.getString("mobile_number"),
        rs.getBoolean("is_blocked"),
        rs.getString("block_reason"),
        rs.getString("block_source"),
        rs.getString("block_code"),
        rs.getObject("resolved_block_code_id", UUID.class),
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
