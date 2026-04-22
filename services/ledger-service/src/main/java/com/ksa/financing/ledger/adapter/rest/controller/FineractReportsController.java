package com.ksa.financing.ledger.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.ledger.application.dto.*;
import com.ksa.financing.ledger.application.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

/**
 * REST controller for Fineract financial reports.
 * All 10 reports are generated from GL transaction data posted to Fineract.
 * Reports include: Trial Balance, Portfolio Summary, DPD Buckets, Collections,
 * Profit/Revenue, Write-offs, Cash Flow, Investor Reports, Reconciliation, and SAMA Regulatory.
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Fineract Reports", description = "Financial reports generated from GL transaction data")
public class FineractReportsController {

    private final TrialBalanceReportService trialBalanceReportService;
    private final PortfolioSummaryReportService portfolioSummaryReportService;
    private final DPDBucketReportService dpdBucketReportService;
    private final CollectionsReportService collectionsReportService;
    private final ProfitRevenueReportService profitRevenueReportService;
    private final WriteOffProvisionReportService writeOffProvisionReportService;
    private final CashFlowReportService cashFlowReportService;
    private final ReconciliationReportService reconciliationReportService;
    private final LoanDisbursementReportService loanDisbursementReportService;
    private final OverdueLoanReportService overdueLoanReportService;
    private final DueLoanReportService dueLoanReportService;
    private final RepaymentScheduleReportService repaymentScheduleReportService;
    private final LoanBalanceOutstandingReportService loanBalanceOutstandingReportService;
    private final CustomerStatementOfAccountService customerStatementOfAccountService;
    private final ProductWisePnLReportService productWisePnLReportService;
    private final CustomerWisePnLReportService customerWisePnLReportService;
    private final LoanHistoryReportService loanHistoryReportService;
    private final DailyTransactionSummaryReportService dailyTransactionSummaryReportService;
    private final NplReportService nplReportService;
    private final EarlySettlementReportService earlySettlementReportService;
    private final WriteOffLoanListReportService writeOffLoanListReportService;
    private final CollectionsDueReportService collectionsDueReportService;
    private final AccountReportService accountReportService;
    private final SimahReportService simahReportService;
    private final JournalVoucherReportService journalVoucherReportService;
    private final DayBookReportService dayBookReportService;
    private final LedgerReportService ledgerReportService;

    /**
     * 1. Trial Balance Report
     * Shows all GL accounts and their balances (debits should equal credits).
     */
    @SecuredEndpoint(obj = "reports.trial-balance", act = "read")
    @GetMapping("/trial-balance")
    @Operation(summary = "Trial Balance Report",
            description = "All GL accounts with their opening balance, transactions, and closing balance")
    @ApiResponse(responseCode = "200", description = "Trial balance report")
    public ResponseEntity<TrialBalanceReportResponse> getTrialBalance(
            @Parameter(description = "Report date", example = "2026-03-30")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        TrialBalanceReportResponse response = trialBalanceReportService.generate(tenantId, date);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. Loan Portfolio Summary Report
     * Shows portfolio health: total disbursed, outstanding, collections, delinquency.
     */
    @SecuredEndpoint(obj = "reports.portfolio", act = "read")
    @GetMapping("/portfolio-summary")
    @Operation(summary = "Loan Portfolio Summary",
            description = "Portfolio overview including disbursed amount, outstanding, collections, and health metrics")
    public ResponseEntity<PortfolioSummaryReportResponse> getPortfolioSummary(
            @Parameter(description = "From date", example = "2026-01-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "To date", example = "2026-03-30")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        LocalDate reportTo = to != null ? to : LocalDate.now(ZoneId.of("UTC"));
        LocalDate reportFrom = from != null ? from : reportTo.minusMonths(1);

        PortfolioSummaryReportResponse response = portfolioSummaryReportService.generate(tenantId, reportFrom, reportTo);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. DPD Bucket Report
     * Delinquency analysis: loans bucketed by days past due with trend.
     */
    @SecuredEndpoint(obj = "reports.dpd", act = "read")
    @GetMapping("/dpd-buckets")
    @Operation(summary = "DPD Bucket Report",
            description = "Loans by delinquency status with trend analysis")
    public ResponseEntity<DPDBucketReportResponse> getDPDBuckets(
            @Parameter(description = "Report date", example = "2026-03-30")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        LocalDate reportDate = date != null ? date : LocalDate.now(ZoneId.of("UTC"));

        DPDBucketReportResponse response = dpdBucketReportService.generate(tenantId, reportDate);
        return ResponseEntity.ok(response);
    }

    /**
     * 4. Collections & Repayment Report
     * Payment collections analysis: methods, performance, on-time vs late.
     */
    @SecuredEndpoint(obj = "reports.collections", act = "read")
    @GetMapping("/collections")
    @Operation(summary = "Collections & Repayment Report",
            description = "Payment collections by method, on-time performance, and collection rate")
    public ResponseEntity<CollectionsReportResponse> getCollectionsReport(
            @Parameter(description = "From date", example = "2026-03-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "To date", example = "2026-03-30")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        LocalDate reportTo = to != null ? to : LocalDate.now(ZoneId.of("UTC"));
        LocalDate reportFrom = from != null ? from : reportTo.withDayOfMonth(1);

        CollectionsReportResponse response = collectionsReportService.generate(tenantId, reportFrom, reportTo);
        return ResponseEntity.ok(response);
    }

    /**
     * 5. Profit & Revenue Report
     * Profit earned vs collected, by product type.
     */
    @SecuredEndpoint(obj = "reports.profit", act = "read")
    @GetMapping("/profit-revenue")
    @Operation(summary = "Profit & Revenue Report",
            description = "Profit earned and collected by product type with quality metrics")
    public ResponseEntity<ProfitRevenueReportResponse> getProfitRevenue(
            @Parameter(description = "Year-Month", example = "2026-03")
            @RequestParam String period,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        ProfitRevenueReportResponse response = profitRevenueReportService.generate(tenantId, period);
        return ResponseEntity.ok(response);
    }

    /**
     * 6. Write-off & Provisions Report
     * Bad debt provisions and restructured loans tracking.
     */
    @SecuredEndpoint(obj = "reports.writeoff", act = "read")
    @GetMapping("/write-off-provisions")
    @Operation(summary = "Write-off & Provisions Report",
            description = "Bad debt provisions, write-offs, and restructured loans")
    public ResponseEntity<WriteOffProvisionReportResponse> getWriteOffProvisions(
            @Parameter(description = "Year-Month", example = "2026-03")
            @RequestParam String period,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        WriteOffProvisionReportResponse response = writeOffProvisionReportService.generate(tenantId, period);
        return ResponseEntity.ok(response);
    }

    /**
     * 7. Cash Flow Report
     * Bank account liquidity analysis: inflows, outflows, net position.
     */
    @SecuredEndpoint(obj = "reports.cashflow", act = "read")
    @GetMapping("/cash-flow")
    @Operation(summary = "Cash Flow Report",
            description = "Bank account inflows and outflows with liquidity position")
    public ResponseEntity<CashFlowReportResponse> getCashFlow(
            @Parameter(description = "Report date", example = "2026-03-30")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        LocalDate reportDate = date != null ? date : LocalDate.now(ZoneId.of("UTC"));

        CashFlowReportResponse response = cashFlowReportService.generate(tenantId, reportDate);
        return ResponseEntity.ok(response);
    }

    /**
     * 8. Reconciliation Report
     * Compare our GL entries with Fineract GL to ensure consistency.
     */
    @SecuredEndpoint(obj = "reports.reconciliation", act = "read")
    @GetMapping("/reconciliation-detail")
    @Operation(summary = "Reconciliation Report",
            description = "Our system GL entries vs Fineract GL entries verification")
    public ResponseEntity<ReconciliationReportDetailResponse> getReconciliationDetail(
            @Parameter(description = "Report date", example = "2026-03-30")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        LocalDate reportDate = date != null ? date : LocalDate.now(ZoneId.of("UTC"));

        ReconciliationReportDetailResponse response = reconciliationReportService.generate(tenantId, reportDate);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Loan-level Operational Reports (Phase 1)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 9. Loan Disbursement Report
     * Lists all loans disbursed within the date range (application#, customer, product, amount, tenure, status, branch).
     */
    @SecuredEndpoint(obj = "reports.loan-disbursement", act = "read")
    @GetMapping("/loan-disbursement")
    @Operation(summary = "Loan Disbursement Report",
            description = "Loans disbursed within the specified date range, with customer, product, amount, tenure, and channel")
    public ResponseEntity<LoanDisbursementReportResponse> getLoanDisbursementReport(
            @Parameter(description = "From date", example = "2026-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @Parameter(description = "To date", example = "2026-03-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @Parameter(description = "Product code filter (optional)")
            @RequestParam(required = false) String productCode,

            @Parameter(description = "Branch or channel filter (optional)")
            @RequestParam(required = false) String branchOrChannel,

            @Parameter(description = "Loan status filter (optional)")
            @RequestParam(required = false) String status,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        LoanDisbursementReportResponse response = loanDisbursementReportService.generate(
                tenantId, fromDate, toDate, productCode, branchOrChannel, status);
        return ResponseEntity.ok(response);
    }

    /**
     * 10. Overdue Loan Report
     * Lists loans with past-due installments, grouped by DPD bucket.
     */
    @SecuredEndpoint(obj = "reports.overdue-loans", act = "read")
    @GetMapping("/overdue-loans")
    @Operation(summary = "Overdue Loan Report",
            description = "Loans with unpaid installments past their due date, including DPD bucket and overdue amounts")
    public ResponseEntity<OverdueLoanReportResponse> getOverdueLoanReport(
            @Parameter(description = "As-of date", example = "2026-03-30")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,

            @Parameter(description = "Minimum days past due", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer minDaysPastDue,

            @Parameter(description = "Product code filter (optional)")
            @RequestParam(required = false) String productCode,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        LocalDate reportDate = asOfDate != null ? asOfDate : LocalDate.now(ZoneId.of("UTC"));

        OverdueLoanReportResponse response = overdueLoanReportService.generate(
                tenantId, reportDate, minDaysPastDue, productCode);
        return ResponseEntity.ok(response);
    }

    /**
     * 11. Due Loan Report
     * Lists upcoming installments in the selected window (for collections planning).
     */
    @SecuredEndpoint(obj = "reports.due-loans", act = "read")
    @GetMapping("/due-loans")
    @Operation(summary = "Due Loan Report",
            description = "Upcoming installments falling due within the selected date window")
    public ResponseEntity<DueLoanReportResponse> getDueLoanReport(
            @Parameter(description = "Window from date", example = "2026-04-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @Parameter(description = "Window to date", example = "2026-04-30")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        DueLoanReportResponse response = dueLoanReportService.generate(tenantId, fromDate, toDate);
        return ResponseEntity.ok(response);
    }

    /**
     * 12. Repayment Schedule Report
     * Full installment plan for a single loan (principal/profit/penalty per installment).
     */
    @SecuredEndpoint(obj = "reports.repayment-schedule", act = "read")
    @GetMapping("/repayment-schedule")
    @Operation(summary = "Repayment Schedule Report (All Loans)",
            description = "Installment plans for all loans in selected date range. Use this endpoint when loanId is not provided.")
    public ResponseEntity<List<RepaymentScheduleReportResponse>> getRepaymentScheduleReportAll(
            @Parameter(description = "From date", example = "2026-04-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @Parameter(description = "To date", example = "2026-04-30")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        LocalDate reportTo = toDate != null ? toDate : LocalDate.now(ZoneId.of("UTC"));
        LocalDate reportFrom = fromDate != null ? fromDate : reportTo.minusMonths(1);

        List<RepaymentScheduleReportResponse> response =
                repaymentScheduleReportService.generateAll(tenantId, reportFrom, reportTo);
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "reports.repayment-schedule", act = "read")
    @GetMapping("/repayment-schedule/{loanId}")
    @Operation(summary = "Repayment Schedule Report",
            description = "Installment plan for a single loan — principal, profit, penalty, remaining principal, payment status per installment")
    public ResponseEntity<RepaymentScheduleReportResponse> getRepaymentScheduleReport(
            @Parameter(description = "Loan ID", required = true)
            @PathVariable("loanId") UUID loanId,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        RepaymentScheduleReportResponse response = repaymentScheduleReportService.generate(tenantId, loanId);
        return ResponseEntity.ok(response);
    }

    /**
     * 13. Loan Balance & Outstanding Report
     * Live outstanding principal / profit / penalties across the portfolio as of a given date.
     */
    @SecuredEndpoint(obj = "reports.loan-balance-outstanding", act = "read")
    @GetMapping("/loan-balance-outstanding")
    @Operation(summary = "Loan Balance & Outstanding Report",
            description = "Per-loan outstanding principal, profit, and penalties with next due date")
    public ResponseEntity<LoanBalanceOutstandingReportResponse> getLoanBalanceOutstandingReport(
            @Parameter(description = "As-of date", example = "2026-03-30")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,

            @Parameter(description = "Filter by customer ID (optional)")
            @RequestParam(required = false) UUID customerId,

            @Parameter(description = "Filter by product code (optional)")
            @RequestParam(required = false) String productCode,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        LocalDate reportDate = asOfDate != null ? asOfDate : LocalDate.now(ZoneId.of("UTC"));

        LoanBalanceOutstandingReportResponse response = loanBalanceOutstandingReportService.generate(
                tenantId, reportDate, customerId, productCode);
        return ResponseEntity.ok(response);
    }

    /**
     * 14. Customer Statement of Account
     * Chronological ledger of all debits/credits for a customer's loan accounts within a date window.
     */
    @SecuredEndpoint(obj = "reports.customer-statement", act = "read")
    @GetMapping("/customer-statement/{customerId}")
    @Operation(summary = "Customer Statement of Account",
            description = "Chronological debit/credit ledger for a customer's loan accounts with opening and closing balance")
    public ResponseEntity<CustomerStatementOfAccountResponse> getCustomerStatementOfAccount(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable("customerId") UUID customerId,

            @Parameter(description = "Statement from date", example = "2026-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @Parameter(description = "Statement to date", example = "2026-03-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        CustomerStatementOfAccountResponse response = customerStatementOfAccountService.generate(
                tenantId, customerId, fromDate, toDate);
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "reports.product-wise-pnl", act = "read")
    @GetMapping("/product-wise-pnl")
    @Operation(summary = "Product-Wise P&L Report",
            description = "P&L summary grouped by product code and product name")
    public ResponseEntity<ProductWisePnLReportResponse> getProductWisePnLReport(
            @RequestParam String period,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        ProductWisePnLReportResponse response = productWisePnLReportService.generate(tenantId, period);
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "reports.customer-wise-pnl", act = "read")
    @GetMapping("/customer-wise-pnl")
    @Operation(summary = "Customer-Wise P&L Report",
            description = "P&L summary grouped by customer")
    public ResponseEntity<CustomerWisePnLReportResponse> getCustomerWisePnLReport(
            @RequestParam String period,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        CustomerWisePnLReportResponse response = customerWisePnLReportService.generate(tenantId, period);
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "reports.loan-history", act = "read")
    @GetMapping("/loan-history/{loanId}")
    @Operation(summary = "Loan History Report",
            description = "Timeline of loan lifecycle events for a specific loan")
    public ResponseEntity<LoanHistoryReportResponse> getLoanHistoryReport(
            @PathVariable("loanId") UUID loanId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        LoanHistoryReportResponse response = loanHistoryReportService.generate(tenantId, loanId);
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "reports.daily-transaction-summary", act = "read")
    @GetMapping("/daily-transaction-summary")
    @Operation(summary = "Daily Transaction Summary Report",
            description = "Daily summary of credits, debits, and transaction counts")
    public ResponseEntity<DailyTransactionSummaryReportResponse> getDailyTransactionSummaryReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        DailyTransactionSummaryReportResponse response = dailyTransactionSummaryReportService.generate(tenantId, date);
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "reports.npl", act = "read")
    @GetMapping("/npl")
    @Operation(summary = "NPL Report",
            description = "Non-performing loan report with NPL ratio and bucket breakdown")
    public ResponseEntity<NplReportResponse> getNplReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        LocalDate reportDate = asOfDate != null ? asOfDate : LocalDate.now(ZoneId.of("UTC"));
        NplReportResponse response = nplReportService.generate(tenantId, reportDate);
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "reports.early-settlement", act = "read")
    @GetMapping("/early-settlement")
    @Operation(summary = "Early Settlement Report",
            description = "Loans settled early with rebate and closure details")
    public ResponseEntity<EarlySettlementReportResponse> getEarlySettlementReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        EarlySettlementReportResponse response = earlySettlementReportService.generate(tenantId, fromDate, toDate);
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "reports.write-off-loans", act = "read")
    @GetMapping("/write-off-loans")
    @Operation(summary = "Write-Off Loan Report (List)",
            description = "List view of write-off loans with principal and provision impact")
    public ResponseEntity<WriteOffLoanListReportResponse> getWriteOffLoanListReport(
            @RequestParam String period,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        WriteOffLoanListReportResponse response = writeOffLoanListReportService.generate(tenantId, period);
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "reports.collections-due", act = "read")
    @GetMapping("/collections-due")
    @Operation(summary = "Collections Due Report",
            description = "Installments due for collection in the selected date range")
    public ResponseEntity<CollectionsDueReportResponse> getCollectionsDueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        CollectionsDueReportResponse response = collectionsDueReportService.generate(tenantId, fromDate, toDate);
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "reports.account", act = "read")
    @GetMapping("/account")
    @Operation(summary = "Account Report",
            description = "GL account-level activity and balances for a selected date")
    public ResponseEntity<AccountReportResponse> getAccountReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        LocalDate reportDate = asOfDate != null ? asOfDate : LocalDate.now(ZoneId.of("UTC"));
        AccountReportResponse response = accountReportService.generate(tenantId, reportDate);
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "reports.simah", act = "read")
    @GetMapping("/simah")
    @Operation(summary = "Simah Report",
            description = "Simah reporting extract with customer and facility level indicators")
    public ResponseEntity<SimahReportResponse> getSimahReport(
            @RequestParam String period,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        SimahReportResponse response = simahReportService.generate(tenantId, period);
        return ResponseEntity.ok(response);
    }

    /**
     * Journal Vouchers Report
     * Lists all journal entries (vouchers) posted in the date range with their line detail.
     */
    @SecuredEndpoint(obj = "reports.journal-vouchers", act = "read")
    @GetMapping("/journal-vouchers")
    @Operation(summary = "Journal Vouchers Report",
            description = "All posted journal vouchers in the selected date range with debit/credit lines. " +
                    "Optional filters: referenceType (LOAN, DISBURSEMENT, REPAYMENT, etc.) and status (POSTED, REVERSED, PENDING).")
    public ResponseEntity<JournalVoucherReportResponse> getJournalVouchersReport(
            @Parameter(description = "From date", example = "2026-04-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @Parameter(description = "To date", example = "2026-04-30")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @Parameter(description = "Reference type filter (optional)", example = "LOAN")
            @RequestParam(required = false) String referenceType,

            @Parameter(description = "Status filter (optional)", example = "POSTED")
            @RequestParam(required = false) String status,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        JournalVoucherReportResponse response = journalVoucherReportService.generate(
                tenantId, fromDate, toDate, referenceType, status);
        return ResponseEntity.ok(response);
    }

    /**
     * Day Book Report
     * Chronological listing of every debit and credit posted on a given date.
     */
    @SecuredEndpoint(obj = "reports.day-book", act = "read")
    @GetMapping("/day-book")
    @Operation(summary = "Day Book Report",
            description = "Chronological listing of every debit and credit line posted on a single date, " +
                    "one row per journal line, with totals.")
    public ResponseEntity<DayBookReportResponse> getDayBookReport(
            @Parameter(description = "Report date", example = "2026-04-20")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            @Parameter(description = "From date (optional range mode)", example = "2026-04-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @Parameter(description = "To date (optional range mode)", example = "2026-04-30")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);

        DayBookReportResponse response;
        if (fromDate != null && toDate != null) {
            response = dayBookReportService.generate(tenantId, fromDate, toDate);
        } else {
            LocalDate reportDate = date != null ? date : LocalDate.now(ZoneId.of("UTC"));
            response = dayBookReportService.generate(tenantId, reportDate);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Ledger (General Ledger) Report
     * Per-account T-account view with opening/closing balances and running balance.
     */
    @SecuredEndpoint(obj = "reports.ledger", act = "read")
    @GetMapping("/ledger")
    @Operation(summary = "Ledger (General Ledger) Report",
            description = "Per-account ledger view with opening balance, chronological debits/credits, " +
                    "running balance, and closing balance. If accountCode or accountId is provided, returns " +
                    "a single-account ledger; otherwise returns every account with activity in the range.")
    public ResponseEntity<LedgerReportResponse> getLedgerReport(
            @Parameter(description = "From date", example = "2026-04-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @Parameter(description = "To date", example = "2026-04-30")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @Parameter(description = "Account code filter (optional)", example = "1000001")
            @RequestParam(required = false) String accountCode,

            @Parameter(description = "Account ID filter (optional)")
            @RequestParam(required = false) UUID accountId,

            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        LedgerReportResponse response = ledgerReportService.generate(
                tenantId, fromDate, toDate, accountCode, accountId);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private UUID extractTenantId(Jwt jwt) {
        String tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null || tenantClaim.isBlank()) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        try {
            return UUID.fromString(tenantClaim);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "Invalid tenant_id claim format in JWT token");
        }
    }
}
