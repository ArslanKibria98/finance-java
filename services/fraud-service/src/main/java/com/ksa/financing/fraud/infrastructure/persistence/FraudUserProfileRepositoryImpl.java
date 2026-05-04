package com.ksa.financing.fraud.infrastructure.persistence;

import com.ksa.financing.fraud.domain.port.out.FraudUserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FraudUserProfileRepositoryImpl implements FraudUserProfileRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<FraudUserProfileView> findByCustomerId(UUID tenantId, String customerId) {
        var results = jdbcTemplate.query(
                "SELECT * FROM fraud_user_profiles WHERE tenant_id = ? AND customer_id = ?",
                MAPPER, tenantId, customerId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public void updateLastLogin(UUID tenantId, String customerId, LocalDateTime loginAt,
                                BigDecimal latitude, BigDecimal longitude,
                                String country, String city, String deviceId) {
        jdbcTemplate.update("""
            UPDATE fraud_user_profiles
               SET last_login_at = ?, last_login_latitude = ?, last_login_longitude = ?,
                   last_login_country = ?, last_login_city = ?, last_login_device_id = ?,
                   last_activity_at = ?, updated_at = NOW()
             WHERE tenant_id = ? AND customer_id = ?
            """,
            Timestamp.valueOf(loginAt), latitude, longitude, country, city, deviceId,
            Timestamp.valueOf(loginAt), tenantId, customerId);
    }

    @Override
    public void incrementLoanApplicationCount(UUID tenantId, String customerId) {
        jdbcTemplate.update(
                "UPDATE fraud_user_profiles SET total_loan_applications = total_loan_applications + 1, " +
                "updated_at = NOW() WHERE tenant_id = ? AND customer_id = ?",
                tenantId, customerId);
    }

    @Override
    public void updateLastActivity(UUID tenantId, String customerId, LocalDateTime activityAt) {
        jdbcTemplate.update(
                "UPDATE fraud_user_profiles SET last_activity_at = ?, updated_at = NOW() " +
                "WHERE tenant_id = ? AND customer_id = ?",
                Timestamp.valueOf(activityAt), tenantId, customerId);
    }

    private static final RowMapper<FraudUserProfileView> MAPPER = (rs, rowNum) -> new FraudUserProfileView(
            rs.getString("customer_id"),
            rs.getString("national_id_hash"),
            rs.getString("nationality"),
            rs.getString("registered_address_city"),
            rs.getString("registered_address_country"),
            rs.getString("registered_iban"),
            rs.getBoolean("iban_verified"),
            rs.getString("iban_holder_name"),
            rs.getString("aml_risk_level"),
            rs.getTimestamp("last_login_at") != null ? rs.getTimestamp("last_login_at").toLocalDateTime() : null,
            rs.getBigDecimal("last_login_latitude"),
            rs.getBigDecimal("last_login_longitude"),
            rs.getString("last_login_country"),
            rs.getString("last_login_city"),
            rs.getString("last_login_device_id"),
            rs.getTimestamp("account_created_at") != null ? rs.getTimestamp("account_created_at").toLocalDateTime() : null,
            rs.getTimestamp("last_activity_at") != null ? rs.getTimestamp("last_activity_at").toLocalDateTime() : null,
            rs.getInt("total_loan_applications"),
            rs.getInt("total_disbursements"),
            rs.getInt("total_repayments"),
            rs.getBoolean("is_dormant")
    );
}
