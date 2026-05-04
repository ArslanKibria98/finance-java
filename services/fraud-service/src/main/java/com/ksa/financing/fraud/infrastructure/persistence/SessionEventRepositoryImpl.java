package com.ksa.financing.fraud.infrastructure.persistence;

import com.ksa.financing.fraud.domain.model.session.SessionEvent;
import com.ksa.financing.fraud.domain.port.out.SessionEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SessionEventRepositoryImpl implements SessionEventRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public SessionEvent save(SessionEvent event) {
        var id = event.id() != null ? event.id() : UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO session_events (id, tenant_id, customer_id, session_id, device_id,
                ip_address, latitude, longitude, country, city, login_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            id, event.tenantId(), event.customerId(), event.sessionId(), event.deviceId(),
            event.ipAddress(), event.latitude(), event.longitude(), event.country(), event.city(),
            Timestamp.valueOf(event.loginAt()));
        return new SessionEvent(id, event.tenantId(), event.customerId(), event.sessionId(),
                event.deviceId(), event.ipAddress(), event.latitude(), event.longitude(),
                event.country(), event.city(), event.loginAt());
    }

    @Override
    public Optional<SessionEvent> findLastByCustomer(UUID tenantId, String customerId) {
        var results = jdbcTemplate.query(
                "SELECT * FROM session_events WHERE tenant_id = ? AND customer_id = ? " +
                "ORDER BY login_at DESC LIMIT 1",
                MAPPER, tenantId, customerId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public List<SessionEvent> findByCustomerSince(UUID tenantId, String customerId, LocalDateTime since) {
        return jdbcTemplate.query(
                "SELECT * FROM session_events WHERE tenant_id = ? AND customer_id = ? AND login_at >= ? " +
                "ORDER BY login_at DESC",
                MAPPER, tenantId, customerId, Timestamp.valueOf(since));
    }

    @Override
    public long countByCustomerSince(UUID tenantId, String customerId, LocalDateTime since) {
        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM session_events WHERE tenant_id = ? AND customer_id = ? AND login_at >= ?",
                Long.class, tenantId, customerId, Timestamp.valueOf(since));
        return count != null ? count : 0;
    }

    @Override
    public Optional<SessionEvent> findLastByCustomerAndDevice(UUID tenantId, String customerId, String deviceId) {
        var results = jdbcTemplate.query(
                "SELECT * FROM session_events WHERE tenant_id = ? AND customer_id = ? AND device_id = ? " +
                "ORDER BY login_at DESC LIMIT 1",
                MAPPER, tenantId, customerId, deviceId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public long countDistinctCustomersByDevice(UUID tenantId, String deviceId, LocalDateTime since) {
        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT customer_id) FROM session_events " +
                "WHERE tenant_id = ? AND device_id = ? AND login_at >= ?",
                Long.class, tenantId, deviceId, Timestamp.valueOf(since));
        return count != null ? count : 0;
    }

    private static final RowMapper<SessionEvent> MAPPER = (rs, rowNum) -> new SessionEvent(
            rs.getObject("id", UUID.class),
            rs.getObject("tenant_id", UUID.class),
            rs.getString("customer_id"),
            rs.getString("session_id"),
            rs.getString("device_id"),
            rs.getString("ip_address"),
            rs.getBigDecimal("latitude"),
            rs.getBigDecimal("longitude"),
            rs.getString("country"),
            rs.getString("city"),
            rs.getTimestamp("login_at").toLocalDateTime()
    );
}
