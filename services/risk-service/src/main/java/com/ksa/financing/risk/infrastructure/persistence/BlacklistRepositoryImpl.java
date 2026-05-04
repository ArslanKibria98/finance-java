package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.domain.valueobject.NationalId;
import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.BlacklistStatus;
import com.ksa.financing.risk.domain.model.MobileBlacklistEntry;
import com.ksa.financing.risk.domain.model.NidBlacklistEntry;
import com.ksa.financing.risk.domain.port.out.BlacklistRepository;
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
public class BlacklistRepositoryImpl implements BlacklistRepository {

    private final JdbcTemplate jdbcTemplate;

    // ===== NID =====

    @Override
    public NidBlacklistEntry saveNid(NidBlacklistEntry entry) {
        var id = entry.id() != null ? entry.id() : UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO nid_blacklist (id, national_id, reason, status, added_by, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (national_id) DO UPDATE SET
                reason = EXCLUDED.reason,
                status = EXCLUDED.status,
                updated_at = EXCLUDED.updated_at
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
    public PageResponse<NidBlacklistEntry> findAllNid(PageQuery pageQuery) {
        String countSql = "SELECT count(*) FROM nid_blacklist";
        long totalElements = Optional.ofNullable(jdbcTemplate.queryForObject(countSql, Long.class)).orElse(0L);

        String sql = "SELECT * FROM nid_blacklist ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<NidBlacklistEntry> content = jdbcTemplate.query(sql, NID_MAPPER, pageQuery.size(), pageQuery.page() * pageQuery.size());

        return new PageResponse<>(content, buildMetadata(pageQuery, totalElements));
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
        var id = entry.id() != null ? entry.id() : UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO mobile_blacklist (id, mobile_number, reason, status, added_by, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (mobile_number) DO UPDATE SET
                reason = EXCLUDED.reason,
                status = EXCLUDED.status,
                updated_at = EXCLUDED.updated_at
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
    public PageResponse<MobileBlacklistEntry> findAllMobile(PageQuery pageQuery) {
        String countSql = "SELECT count(*) FROM mobile_blacklist";
        long totalElements = Optional.ofNullable(jdbcTemplate.queryForObject(countSql, Long.class)).orElse(0L);

        String sql = "SELECT * FROM mobile_blacklist ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<MobileBlacklistEntry> content = jdbcTemplate.query(sql, MOBILE_MAPPER, pageQuery.size(), pageQuery.page() * pageQuery.size());

        return new PageResponse<>(content, buildMetadata(pageQuery, totalElements));
    }

    @Override
    public boolean isMobileBlacklisted(String mobileNumber) {
        var count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM mobile_blacklist WHERE mobile_number = ? AND status = 'BLACKLISTED'",
            Integer.class, mobileNumber
        );
        return count != null && count > 0;
    }

    private PageMetadata buildMetadata(PageQuery query, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / query.size());
        return new PageMetadata(
                query.page(),
                query.size(),
                totalElements,
                totalPages,
                query.page() == 0,
                query.page() >= totalPages - 1,
                totalElements == 0
        );
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
