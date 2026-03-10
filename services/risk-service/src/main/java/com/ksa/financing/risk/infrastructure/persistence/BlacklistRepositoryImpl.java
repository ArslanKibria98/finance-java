package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.domain.valueobject.NationalId;
import com.ksa.financing.risk.domain.model.BlacklistStatus;
import com.ksa.financing.risk.domain.model.MobileBlacklistEntry;
import com.ksa.financing.risk.domain.model.NidBlacklistEntry;
import com.ksa.financing.risk.domain.port.out.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class BlacklistRepositoryImpl implements BlacklistRepository {

    private final JdbcTemplate jdbcTemplate;

    // ===== NID =====

    @Override
    public NidBlacklistEntry saveNid(NidBlacklistEntry entry) {
        var id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO nid_blacklist (id, national_id, reason, status, added_by, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            id, entry.nationalId().value(), entry.reason(), entry.status().name(),
            entry.addedBy(), Timestamp.from(entry.createdAt()), Timestamp.from(entry.updatedAt())
        );
        return new NidBlacklistEntry(id, entry.nationalId(), entry.reason(), entry.status(),
            entry.addedBy(), entry.createdAt(), entry.updatedAt());
    }

    @Override
    public Optional<NidBlacklistEntry> findNidByNationalId(String nationalId) {
        var results = jdbcTemplate.query(
            "SELECT * FROM nid_blacklist WHERE national_id = ?", NID_MAPPER, nationalId
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public void updateNidStatus(String nationalId, BlacklistStatus status) {
        jdbcTemplate.update(
            "UPDATE nid_blacklist SET status = ?, updated_at = ? WHERE national_id = ?",
            status.name(), Timestamp.from(Instant.now()), nationalId
        );
    }

    @Override
    public List<NidBlacklistEntry> findAllNid() {
        return jdbcTemplate.query(
            "SELECT * FROM nid_blacklist ORDER BY created_at DESC", NID_MAPPER
        );
    }

    @Override
    public boolean isNidBlacklisted(String nationalId) {
        var count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM nid_blacklist WHERE national_id = ? AND status = 'BLACKLISTED'",
            Integer.class, nationalId
        );
        return count != null && count > 0;
    }

    // ===== MOBILE =====

    @Override
    public MobileBlacklistEntry saveMobile(MobileBlacklistEntry entry) {
        var id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO mobile_blacklist (id, mobile_number, reason, status, added_by, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            id, entry.mobileNumber(), entry.reason(), entry.status().name(),
            entry.addedBy(), Timestamp.from(entry.createdAt()), Timestamp.from(entry.updatedAt())
        );
        return new MobileBlacklistEntry(id, entry.mobileNumber(), entry.reason(), entry.status(),
            entry.addedBy(), entry.createdAt(), entry.updatedAt());
    }

    @Override
    public Optional<MobileBlacklistEntry> findMobileByNumber(String mobileNumber) {
        var results = jdbcTemplate.query(
            "SELECT * FROM mobile_blacklist WHERE mobile_number = ?", MOBILE_MAPPER, mobileNumber
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public void updateMobileStatus(String mobileNumber, BlacklistStatus status) {
        jdbcTemplate.update(
            "UPDATE mobile_blacklist SET status = ?, updated_at = ? WHERE mobile_number = ?",
            status.name(), Timestamp.from(Instant.now()), mobileNumber
        );
    }

    @Override
    public List<MobileBlacklistEntry> findAllMobile() {
        return jdbcTemplate.query(
            "SELECT * FROM mobile_blacklist ORDER BY created_at DESC", MOBILE_MAPPER
        );
    }

    @Override
    public boolean isMobileBlacklisted(String mobileNumber) {
        var count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM mobile_blacklist WHERE mobile_number = ? AND status = 'BLACKLISTED'",
            Integer.class, mobileNumber
        );
        return count != null && count > 0;
    }

    // ===== ROW MAPPERS =====

    private static final RowMapper<NidBlacklistEntry> NID_MAPPER = (rs, rowNum) -> new NidBlacklistEntry(
        rs.getObject("id", UUID.class),
        NationalId.of(rs.getString("national_id")),
        rs.getString("reason"),
        BlacklistStatus.valueOf(rs.getString("status")),
        rs.getString("added_by"),
        toInstant(rs.getTimestamp("created_at")),
        toInstant(rs.getTimestamp("updated_at"))
    );

    private static final RowMapper<MobileBlacklistEntry> MOBILE_MAPPER = (rs, rowNum) -> new MobileBlacklistEntry(
        rs.getObject("id", UUID.class),
        rs.getString("mobile_number"),
        rs.getString("reason"),
        BlacklistStatus.valueOf(rs.getString("status")),
        rs.getString("added_by"),
        toInstant(rs.getTimestamp("created_at")),
        toInstant(rs.getTimestamp("updated_at"))
    );

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
