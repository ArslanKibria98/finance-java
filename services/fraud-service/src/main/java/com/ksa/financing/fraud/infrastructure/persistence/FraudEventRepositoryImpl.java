package com.ksa.financing.fraud.infrastructure.persistence;

import com.ksa.financing.fraud.domain.model.device.DeviceInfo;
import com.ksa.financing.fraud.domain.model.device.DeviceIntegrityStatus;
import com.ksa.financing.fraud.domain.model.device.DeviceOS;
import com.ksa.financing.fraud.domain.model.device.DeviceType;
import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.fraud.FraudEventType;
import com.ksa.financing.fraud.domain.model.location.LocationData;
import com.ksa.financing.fraud.domain.model.transaction.PaymentSource;
import com.ksa.financing.fraud.domain.port.out.FraudEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FraudEventRepositoryImpl implements FraudEventRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public FraudEvent save(FraudEvent event) {
        var id = event.id() != null ? event.id() : UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO fraud_events (id, tenant_id, event_id, event_type, customer_id, national_id_hash,
                device_id, device_type, device_os, os_version, device_fingerprint, device_integrity,
                latitude, longitude, ip_address, ip_country, ip_city, gps_country, gps_city,
                vpn_detected, proxy_detected, session_id, event_timestamp,
                transaction_type, transaction_amount, currency,
                loan_application_id, loan_product_type, approved_loan_amount,
                disbursement_iban, iban_verification_status, iban_holder_name,
                payment_iban, card_last4, card_country, card_holder_name, is_third_party_payment,
                resolved_country, resolved_city, distance_from_last_km, time_since_last_login_minutes,
                correlation_id, received_at)
            VALUES (?,?,?,?::fraud_event_type,?,?, ?,?::device_type_enum,?::device_os_enum,?,?,?::device_integrity_enum,
                ?,?,?,?,?,?,?, ?,?,?,?, ?,?,?, ?,?,?, ?,?,?, ?,?,?,?,?, ?,?,?,?, ?,?)
            ON CONFLICT (tenant_id, event_id) DO NOTHING
            """,
            id, event.tenantId(), event.eventId(),
            event.eventType() != null ? event.eventType().name() : null,
            event.customerId(), event.nationalIdHash(),
            event.deviceInfo() != null ? event.deviceInfo().deviceId() : null,
            event.deviceInfo() != null && event.deviceInfo().deviceType() != null ? event.deviceInfo().deviceType().name() : null,
            event.deviceInfo() != null && event.deviceInfo().deviceOs() != null ? event.deviceInfo().deviceOs().name() : null,
            event.deviceInfo() != null ? event.deviceInfo().osVersion() : null,
            event.deviceInfo() != null ? event.deviceInfo().deviceFingerprint() : null,
            event.deviceInfo() != null && event.deviceInfo().integrityStatus() != null ? event.deviceInfo().integrityStatus().name() : null,
            event.locationData() != null ? event.locationData().latitude() : null,
            event.locationData() != null ? event.locationData().longitude() : null,
            event.locationData() != null ? event.locationData().ipAddress() : null,
            event.locationData() != null ? event.locationData().ipCountry() : null,
            event.locationData() != null ? event.locationData().ipCity() : null,
            event.locationData() != null ? event.locationData().gpsCountry() : null,
            event.locationData() != null ? event.locationData().gpsCity() : null,
            event.locationData() != null && event.locationData().vpnDetected(),
            event.locationData() != null && event.locationData().proxyDetected(),
            event.sessionId(),
            Timestamp.valueOf(event.eventTimestamp()),
            event.transactionType(),
            event.transactionAmount(),
            event.currency(),
            event.loanApplicationId(), event.loanProductType(), event.approvedLoanAmount(),
            event.disbursementIban(), event.ibanVerificationStatus(), event.ibanHolderName(),
            event.paymentSource() != null ? event.paymentSource().iban() : null,
            event.paymentSource() != null ? event.paymentSource().cardLast4() : null,
            event.paymentSource() != null ? event.paymentSource().cardCountry() : null,
            event.paymentSource() != null ? event.paymentSource().cardHolderName() : null,
            event.paymentSource() != null && event.paymentSource().thirdParty(),
            event.resolvedCountry(), event.resolvedCity(),
            event.distanceFromLastKm(),
            event.timeSinceLastLogin() != null ? (int) event.timeSinceLastLogin().toMinutes() : null,
            event.correlationId(),
            Timestamp.valueOf(event.receivedAt() != null ? event.receivedAt() : LocalDateTime.now())
        );

        return new FraudEvent(id, event.tenantId(), event.eventId(), event.eventType(),
                event.customerId(), event.nationalIdHash(), event.deviceInfo(), event.locationData(),
                event.eventTimestamp(), event.sessionId(), event.transactionType(),
                event.transactionAmount(), event.currency(), event.loanApplicationId(),
                event.loanProductType(), event.approvedLoanAmount(), event.disbursementIban(),
                event.ibanVerificationStatus(), event.ibanHolderName(), event.paymentSource(),
                event.resolvedCountry(), event.resolvedCity(), event.distanceFromLastKm(),
                event.timeSinceLastLogin(), event.vpnDetected(), event.proxyDetected(),
                event.receivedAt(), event.correlationId());
    }

    @Override
    public Optional<FraudEvent> findByTenantAndEventId(UUID tenantId, String eventId) {
        var results = jdbcTemplate.query(
                "SELECT * FROM fraud_events WHERE tenant_id = ? AND event_id = ?",
                EVENT_MAPPER, tenantId, eventId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public List<FraudEvent> findByCustomerId(UUID tenantId, String customerId, LocalDateTime since) {
        return jdbcTemplate.query(
                "SELECT * FROM fraud_events WHERE tenant_id = ? AND customer_id = ? AND event_timestamp >= ? ORDER BY event_timestamp DESC",
                EVENT_MAPPER, tenantId, customerId, Timestamp.valueOf(since));
    }

    @Override
    public List<FraudEvent> findByDeviceId(String deviceId, LocalDateTime since) {
        return jdbcTemplate.query(
                "SELECT * FROM fraud_events WHERE device_id = ? AND event_timestamp >= ? ORDER BY event_timestamp DESC",
                EVENT_MAPPER, deviceId, Timestamp.valueOf(since));
    }

    @Override
    public List<FraudEvent> findByDisbursementIban(String iban, LocalDateTime since) {
        return jdbcTemplate.query(
                "SELECT * FROM fraud_events WHERE disbursement_iban = ? AND event_timestamp >= ? ORDER BY event_timestamp DESC",
                EVENT_MAPPER, iban, Timestamp.valueOf(since));
    }

    @Override
    public long countByCustomerAndType(UUID tenantId, String customerId, String eventType, LocalDateTime since) {
        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fraud_events WHERE tenant_id = ? AND customer_id = ? AND event_type = ?::fraud_event_type AND event_timestamp >= ?",
                Long.class, tenantId, customerId, eventType, Timestamp.valueOf(since));
        return count != null ? count : 0;
    }

    @Override
    public long countDistinctCustomersByDeviceId(String deviceId, LocalDateTime since) {
        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT customer_id) FROM fraud_events WHERE device_id = ? AND event_timestamp >= ?",
                Long.class, deviceId, Timestamp.valueOf(since));
        return count != null ? count : 0;
    }

    @Override
    public long countByCustomerAndTypeBetween(UUID tenantId, String customerId, String eventType,
                                              LocalDateTime from, LocalDateTime to) {
        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fraud_events WHERE tenant_id = ? AND customer_id = ? " +
                "AND event_type = ?::fraud_event_type AND event_timestamp >= ? AND event_timestamp < ?",
                Long.class, tenantId, customerId, eventType,
                Timestamp.valueOf(from), Timestamp.valueOf(to));
        return count != null ? count : 0;
    }

    @Override
    public List<FraudEvent> findDuplicateApplications(UUID tenantId, String customerId,
                                                      String productType, BigDecimal amount,
                                                      LocalDateTime since) {
        return jdbcTemplate.query(
                "SELECT * FROM fraud_events WHERE tenant_id = ? AND customer_id = ? " +
                "AND event_type = 'LOAN_APPLICATION' AND loan_product_type = ? " +
                "AND transaction_amount = ? AND event_timestamp >= ? " +
                "ORDER BY event_timestamp DESC",
                EVENT_MAPPER, tenantId, customerId, productType, amount, Timestamp.valueOf(since));
    }

    @Override
    public long countDistinctCustomersByIban(String iban, LocalDateTime since) {
        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT customer_id) FROM fraud_events " +
                "WHERE disbursement_iban = ? AND event_timestamp >= ?",
                Long.class, iban, Timestamp.valueOf(since));
        return count != null ? count : 0;
    }

    @Override
    public List<FraudEvent> findByIpAddress(String ipAddress, LocalDateTime since) {
        return jdbcTemplate.query(
                "SELECT * FROM fraud_events WHERE ip_address = ? AND event_timestamp >= ? " +
                "ORDER BY event_timestamp DESC",
                EVENT_MAPPER, ipAddress, Timestamp.valueOf(since));
    }

    @Override
    public long countDistinctCardsByCustomer(UUID tenantId, String customerId, LocalDateTime since) {
        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT card_last4) FROM fraud_events " +
                "WHERE tenant_id = ? AND customer_id = ? AND card_last4 IS NOT NULL " +
                "AND event_timestamp >= ?",
                Long.class, tenantId, customerId, Timestamp.valueOf(since));
        return count != null ? count : 0;
    }

    @Override
    public List<FraudEvent> findRecentByType(UUID tenantId, String customerId,
                                             String eventType, int limit) {
        return jdbcTemplate.query(
                "SELECT * FROM fraud_events WHERE tenant_id = ? AND customer_id = ? " +
                "AND event_type = ?::fraud_event_type ORDER BY event_timestamp DESC LIMIT ?",
                EVENT_MAPPER, tenantId, customerId, eventType, limit);
    }

    @Override
    public List<FraudEvent> findLastNByCustomer(UUID tenantId, String customerId, int limit) {
        return jdbcTemplate.query(
                "SELECT * FROM fraud_events WHERE tenant_id = ? AND customer_id = ? " +
                "ORDER BY event_timestamp DESC LIMIT ?",
                EVENT_MAPPER, tenantId, customerId, limit);
    }

    @Override
    public long countReversalsForCustomer(UUID tenantId, String customerId) {
        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fraud_events WHERE tenant_id = ? AND customer_id = ? " +
                "AND transaction_type = 'PAYMENT_REVERSAL'",
                Long.class, tenantId, customerId);
        return count != null ? count : 0;
    }

    @Override
    public long countReversalsForLoan(UUID tenantId, String customerId, String loanApplicationId) {
        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fraud_events WHERE tenant_id = ? AND customer_id = ? " +
                "AND loan_application_id = ? AND transaction_type = 'PAYMENT_REVERSAL'",
                Long.class, tenantId, customerId, loanApplicationId);
        return count != null ? count : 0;
    }

    private static final RowMapper<FraudEvent> EVENT_MAPPER = (rs, rowNum) -> {
        var deviceInfo = new DeviceInfo(
                rs.getString("device_id"),
                parseEnum(DeviceType.class, rs.getString("device_type")),
                parseEnum(DeviceOS.class, rs.getString("device_os")),
                rs.getString("os_version"),
                rs.getString("device_fingerprint"),
                parseEnum(DeviceIntegrityStatus.class, rs.getString("device_integrity"))
        );
        var locationData = new LocationData(
                rs.getBigDecimal("latitude"), rs.getBigDecimal("longitude"),
                rs.getString("ip_address"), rs.getString("ip_country"), rs.getString("ip_city"),
                rs.getString("gps_country"), rs.getString("gps_city"),
                rs.getBoolean("vpn_detected"), rs.getBoolean("proxy_detected")
        );
        PaymentSource paymentSource = null;
        var paymentIban = rs.getString("payment_iban");
        var cardLast4 = rs.getString("card_last4");
        if (paymentIban != null || cardLast4 != null) {
            paymentSource = new PaymentSource(paymentIban, cardLast4,
                    rs.getString("card_country"), rs.getString("card_holder_name"),
                    rs.getBoolean("is_third_party_payment"));
        }
        var tslMinutes = rs.getObject("time_since_last_login_minutes", Integer.class);

        return new FraudEvent(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getString("event_id"),
                parseEnum(FraudEventType.class, rs.getString("event_type")),
                rs.getString("customer_id"),
                rs.getString("national_id_hash"),
                deviceInfo, locationData,
                rs.getTimestamp("event_timestamp").toLocalDateTime(),
                rs.getString("session_id"),
                rs.getString("transaction_type"),
                rs.getBigDecimal("transaction_amount"),
                rs.getString("currency"),
                rs.getString("loan_application_id"),
                rs.getString("loan_product_type"),
                rs.getBigDecimal("approved_loan_amount"),
                rs.getString("disbursement_iban"),
                rs.getString("iban_verification_status"),
                rs.getString("iban_holder_name"),
                paymentSource,
                rs.getString("resolved_country"),
                rs.getString("resolved_city"),
                rs.getBigDecimal("distance_from_last_km"),
                tslMinutes != null ? Duration.ofMinutes(tslMinutes) : null,
                rs.getBoolean("vpn_detected"),
                rs.getBoolean("proxy_detected"),
                rs.getTimestamp("received_at") != null ? rs.getTimestamp("received_at").toLocalDateTime() : null,
                rs.getString("correlation_id")
        );
    };

    private static <T extends Enum<T>> T parseEnum(Class<T> clazz, String value) {
        if (value == null) return null;
        try { return Enum.valueOf(clazz, value); } catch (IllegalArgumentException e) { return null; }
    }
}
