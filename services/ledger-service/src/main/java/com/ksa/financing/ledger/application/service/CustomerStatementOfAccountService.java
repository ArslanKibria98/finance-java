package com.ksa.financing.ledger.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.ledger.application.dto.CustomerStatementOfAccountResponse;
import com.ksa.financing.ledger.application.dto.CustomerStatementOfAccountResponse.Entry;
import com.ksa.financing.ledger.infrastructure.client.ReportDataClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Customer Statement of Account.
 *
 * <p>Strategy:
 * <ol>
 *   <li>Resolve the customer's loan IDs via lending-service ({@code /loans-by-customer}).</li>
 *   <li>Pull every {@code journal_entry} whose {@code reference_id} is one of those loans
 *       in the date window, joined with each line's debit/credit amount.</li>
 *   <li>Compute opening balance (sum before {@code fromDate}), closing balance, totals.</li>
 *   <li>Apply pagination on the in-memory list (entry count is bounded by # of journals).</li>
 * </ol>
 *
 * <p>Empty result is normal when the customer has no loans, no journal entries reference
 * those loans, or the date window excludes everything.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerStatementOfAccountService {

    private final JdbcTemplate jdbcTemplate;
    private final ReportDataClient reportDataClient;

    @Transactional(readOnly = true)
    public CustomerStatementOfAccountResponse generate(UUID tenantId,
                                                       UUID customerId,
                                                       LocalDate fromDate,
                                                       LocalDate toDate,
                                                       PageQuery pageQuery) {
        log.info("Generating customer statement tenant={} customerId={} from={} to={}",
                tenantId, customerId, fromDate, toDate);

        List<UUID> loanIds = resolveCustomerLoans(customerId);
        if (loanIds.isEmpty()) {
            return empty(customerId, fromDate, toDate, pageQuery);
        }

        BigDecimal openingDebit = sumLinesBefore(tenantId, loanIds, fromDate, true);
        BigDecimal openingCredit = sumLinesBefore(tenantId, loanIds, fromDate, false);
        BigDecimal openingBalance = openingDebit.subtract(openingCredit);

        List<RawLine> raw = fetchLinesInRange(tenantId, loanIds, fromDate, toDate, pageQuery.search());

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal running = openingBalance;
        List<Entry> entries = new ArrayList<>(raw.size());
        for (RawLine r : raw) {
            running = running.add(r.debit).subtract(r.credit);
            totalDebits = totalDebits.add(r.debit);
            totalCredits = totalCredits.add(r.credit);
            entries.add(Entry.builder()
                    .transactionDate(r.entryDate)
                    .transactionType(deriveType(r.transactionType, r.referenceType))
                    .description(r.description)
                    .reference(r.referenceId != null ? r.referenceId.toString() : null)
                    .debit(r.debit)
                    .credit(r.credit)
                    .runningBalance(running)
                    .build());
        }
        BigDecimal closingBalance = running;

        // Pagination over the materialised entries (size already bounded; no pre-pagination push-down).
        int page = pageQuery.page();
        int size = pageQuery.size() > 0 ? pageQuery.size() : 20;
        int totalElements = entries.size();
        int fromIdx = Math.min(page * size, totalElements);
        int toIdx = Math.min(fromIdx + size, totalElements);
        List<Entry> paged = entries.subList(fromIdx, toIdx);
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        return CustomerStatementOfAccountResponse.builder()
                .customerId(customerId)
                .customerName(null)
                .nationalId(null)
                .fromDate(fromDate)
                .toDate(toDate)
                .openingBalance(openingBalance)
                .closingBalance(closingBalance)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .entries(paged)
                .pagination(new PageMetadata(page, size, totalElements, totalPages,
                        page == 0, toIdx == totalElements, paged.isEmpty()))
                .build();
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private List<UUID> resolveCustomerLoans(UUID customerId) {
        try {
            JsonNode node = reportDataClient.fetchLoansByCustomer(customerId);
            if (node == null || !node.isArray()) return List.of();
            List<UUID> ids = new ArrayList<>(node.size());
            for (JsonNode n : node) {
                try { ids.add(UUID.fromString(n.asText())); } catch (IllegalArgumentException ignored) {}
            }
            return ids;
        } catch (RuntimeException ex) {
            log.warn("Could not fetch loans for customer {}: {}", customerId, ex.getMessage());
            return List.of();
        }
    }

    private BigDecimal sumLinesBefore(UUID tenantId, List<UUID> loanIds, LocalDate fromDate, boolean debit) {
        String placeholders = String.join(",", Collections.nCopies(loanIds.size(), "?"));
        String column = debit ? "debit_amount" : "credit_amount";
        String sql = "SELECT COALESCE(SUM(jl." + column + "), 0) " +
                "FROM journal_lines jl JOIN journal_entries je ON je.id = jl.journal_entry_id " +
                "WHERE jl.tenant_id = ? AND je.status = 'POSTED' " +
                "AND je.reference_id IN (" + placeholders + ") AND je.entry_date < ?";

        Object[] args = new Object[loanIds.size() + 2];
        args[0] = tenantId;
        for (int i = 0; i < loanIds.size(); i++) args[i + 1] = loanIds.get(i);
        args[args.length - 1] = Date.valueOf(fromDate);

        BigDecimal v = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return v != null ? v : BigDecimal.ZERO;
    }

    private List<RawLine> fetchLinesInRange(UUID tenantId, List<UUID> loanIds,
                                            LocalDate fromDate, LocalDate toDate, String search) {
        String placeholders = String.join(",", Collections.nCopies(loanIds.size(), "?"));
        String searchPattern = (search == null || search.isBlank())
                ? null : "%" + search.trim().toLowerCase() + "%";

        String sql = "SELECT je.entry_date, je.description, je.transaction_type, je.reference_type, je.reference_id, " +
                "       jl.debit_amount, jl.credit_amount " +
                "FROM journal_lines jl JOIN journal_entries je ON je.id = jl.journal_entry_id " +
                "WHERE jl.tenant_id = ? AND je.status = 'POSTED' " +
                "AND je.reference_id IN (" + placeholders + ") " +
                "AND je.entry_date BETWEEN ? AND ? " +
                "AND (CAST(? AS text) IS NULL OR LOWER(COALESCE(je.description, '')) LIKE CAST(? AS text)) " +
                "ORDER BY je.entry_date, je.entry_number, jl.line_number";

        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        args.addAll(loanIds);
        args.add(Date.valueOf(fromDate));
        args.add(Date.valueOf(toDate));
        args.add(searchPattern);
        args.add(searchPattern);

        return jdbcTemplate.query(sql, (rs, rn) -> new RawLine(
                rs.getDate("entry_date").toLocalDate(),
                rs.getString("description"),
                rs.getString("transaction_type"),
                rs.getString("reference_type"),
                rs.getObject("reference_id", UUID.class),
                nz(rs.getBigDecimal("debit_amount")),
                nz(rs.getBigDecimal("credit_amount"))
        ), args.toArray());
    }

    private static String deriveType(String txType, String refType) {
        if (refType == null || refType.isBlank()) return txType != null ? txType : "GENERAL";
        return refType;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private CustomerStatementOfAccountResponse empty(UUID customerId, LocalDate fromDate, LocalDate toDate, PageQuery pq) {
        int size = pq.size() > 0 ? pq.size() : 20;
        return CustomerStatementOfAccountResponse.builder()
                .customerId(customerId).customerName(null).nationalId(null)
                .fromDate(fromDate).toDate(toDate)
                .openingBalance(BigDecimal.ZERO).closingBalance(BigDecimal.ZERO)
                .totalDebits(BigDecimal.ZERO).totalCredits(BigDecimal.ZERO)
                .entries(List.of())
                .pagination(new PageMetadata(pq.page(), size, 0, 0, true, true, true))
                .build();
    }

    private record RawLine(LocalDate entryDate, String description, String transactionType,
                           String referenceType, UUID referenceId,
                           BigDecimal debit, BigDecimal credit) {}
}
