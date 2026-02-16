package com.demo.islamic.compliance.sama;

import com.demo.islamic.compliance.config.ComplianceConfig;
import com.demo.islamic.compliance.model.*;
import com.demo.islamic.payment.model.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates regulatory reports required by SAMA
 * Includes NPL reports, risk reports, and compliance reports
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SamaReportGenerator {

    private final ComplianceConfig complianceConfig;
    private final LoanDataProvider loanDataProvider;
    private final RiskDataProvider riskDataProvider;
    private final SamaAuditLogger auditLogger;

    private static final DateTimeFormatter REPORT_DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Generate monthly Non-Performing Loan (NPL) report
     */
    public NplReport generateMonthlyNplReport(YearMonth reportMonth) {
        log.info("Generating monthly NPL report for: {}", reportMonth);

        NplReport report = new NplReport();
        report.setReportId(UUID.randomUUID().toString());
        report.setReportMonth(reportMonth);
        report.setGeneratedAt(LocalDateTime.now());
        report.setInstitutionId(complianceConfig.getInstitutionId());

        // Get all loans
        List<LoanData> allLoans = loanDataProvider.getLoansForMonth(reportMonth);

        // Calculate NPL metrics
        report.setTotalLoans(allLoans.size());
        report.setTotalOutstanding(calculateTotalOutstanding(allLoans));

        // Classify loans by DPD buckets
        Map<String, List<LoanData>> dpdBuckets = classifyByDpd(allLoans);
        report.setDpdBuckets(createDpdBucketSummary(dpdBuckets));

        // Calculate NPL ratios
        BigDecimal nplAmount = calculateNplAmount(dpdBuckets);
        report.setNplAmount(nplAmount);
        report.setNplRatio(calculateNplRatio(nplAmount, report.getTotalOutstanding()));

        // Stage classification (IFRS 9)
        Map<String, StageClassification> stageClassification = classifyByStage(allLoans);
        report.setStageClassification(stageClassification);

        // Provision coverage
        BigDecimal totalProvisions = calculateProvisions(stageClassification);
        report.setTotalProvisions(totalProvisions);
        report.setProvisionCoverageRatio(
            calculateProvisionCoverage(totalProvisions, nplAmount)
        );

        // Product-wise breakdown
        report.setProductWiseNpl(generateProductWiseNpl(allLoans));

        // Sector-wise breakdown
        report.setSectorWiseNpl(generateSectorWiseNpl(allLoans));

        // Recovery statistics
        report.setRecoveryStatistics(generateRecoveryStatistics(reportMonth));

        // Log report generation
        auditLogger.logReportGeneration(
            "SYSTEM",
            "NPL_REPORT",
            report.getReportId(),
            Map.of("month", reportMonth.toString())
        );

        log.info("NPL report generated: reportId={}, nplRatio={}%",
            report.getReportId(), report.getNplRatio());

        return report;
    }

    /**
     * Generate quarterly risk report
     */
    public RiskReport generateQuarterlyRiskReport(int year, int quarter) {
        log.info("Generating quarterly risk report for Q{} {}", quarter, year);

        RiskReport report = new RiskReport();
        report.setReportId(UUID.randomUUID().toString());
        report.setYear(year);
        report.setQuarter(quarter);
        report.setGeneratedAt(LocalDateTime.now());
        report.setInstitutionId(complianceConfig.getInstitutionId());

        // Credit risk metrics
        CreditRiskMetrics creditRisk = new CreditRiskMetrics();
        creditRisk.setWeightedAverageRiskGrade(
            riskDataProvider.calculateWeightedAverageRiskGrade(year, quarter)
        );
        creditRisk.setExpectedLoss(
            riskDataProvider.calculateExpectedLoss(year, quarter)
        );
        creditRisk.setUnexpectedLoss(
            riskDataProvider.calculateUnexpectedLoss(year, quarter)
        );
        creditRisk.setProbabilityOfDefault(
            riskDataProvider.calculateAveragePd(year, quarter)
        );
        creditRisk.setLossGivenDefault(
            riskDataProvider.calculateAverageLgd(year, quarter)
        );
        report.setCreditRiskMetrics(creditRisk);

        // Concentration risk
        ConcentrationRisk concentration = new ConcentrationRisk();
        concentration.setTop10Borrowers(
            riskDataProvider.getTop10BorrowersExposure(year, quarter)
        );
        concentration.setSectorConcentration(
            riskDataProvider.getSectorConcentration(year, quarter)
        );
        concentration.setGeographicConcentration(
            riskDataProvider.getGeographicConcentration(year, quarter)
        );
        concentration.setProductConcentration(
            riskDataProvider.getProductConcentration(year, quarter)
        );
        report.setConcentrationRisk(concentration);

        // Operational risk
        OperationalRiskMetrics operationalRisk = new OperationalRiskMetrics();
        operationalRisk.setIncidentCount(
            riskDataProvider.getOperationalIncidentCount(year, quarter)
        );
        operationalRisk.setTotalLoss(
            riskDataProvider.getOperationalLoss(year, quarter)
        );
        operationalRisk.setKeyRiskIndicators(
            riskDataProvider.getKeyRiskIndicators(year, quarter)
        );
        report.setOperationalRisk(operationalRisk);

        // Liquidity risk
        LiquidityRiskMetrics liquidityRisk = new LiquidityRiskMetrics();
        liquidityRisk.setLiquidityCoverageRatio(
            riskDataProvider.calculateLcr(year, quarter)
        );
        liquidityRisk.setNetStableFundingRatio(
            riskDataProvider.calculateNsfr(year, quarter)
        );
        liquidityRisk.setCashFlowGap(
            riskDataProvider.calculateCashFlowGap(year, quarter)
        );
        report.setLiquidityRisk(liquidityRisk);

        // Stress testing results
        report.setStressTestResults(
            riskDataProvider.getStressTestResults(year, quarter)
        );

        // Log report generation
        auditLogger.logReportGeneration(
            "SYSTEM",
            "RISK_REPORT",
            report.getReportId(),
            Map.of("year", year, "quarter", quarter)
        );

        log.info("Risk report generated: reportId={}", report.getReportId());

        return report;
    }

    /**
     * Generate annual compliance report
     */
    public ComplianceReport generateAnnualComplianceReport(int year) {
        log.info("Generating annual compliance report for year: {}", year);

        ComplianceReport report = new ComplianceReport();
        report.setReportId(UUID.randomUUID().toString());
        report.setReportYear(year);
        report.setGeneratedAt(LocalDateTime.now());
        report.setInstitutionId(complianceConfig.getInstitutionId());

        // Regulatory compliance status
        RegulatoryCompliance regulatory = new RegulatoryCompliance();
        regulatory.setSamaCompliance(assessSamaCompliance(year));
        regulatory.setNcaCompliance(assessNcaCompliance(year));
        regulatory.setZatcaCompliance(assessZatcaCompliance(year));
        regulatory.setPdplCompliance(assessPdplCompliance(year));
        report.setRegulatoryCompliance(regulatory);

        // AML/CFT compliance
        AmlCftCompliance amlCft = new AmlCftCompliance();
        amlCft.setSuspiciousTransactionReports(
            getSuspiciousTransactionCount(year)
        );
        amlCft.setCustomersDueForKycUpdate(
            getCustomersDueForKycUpdate(year)
        );
        amlCft.setPepScreeningResults(
            getPepScreeningResults(year)
        );
        amlCft.setSanctionScreeningResults(
            getSanctionScreeningResults(year)
        );
        report.setAmlCftCompliance(amlCft);

        // Sharia compliance
        ShariaCompliance sharia = new ShariaCompliance();
        sharia.setProductCompliance(assessProductShariaCompliance(year));
        sharia.setPurificationAmount(calculatePurificationAmount(year));
        sharia.setShariaAuditFindings(getShariaAuditFindings(year));
        report.setShariaCompliance(sharia);

        // Data protection compliance
        DataProtectionCompliance dataProtection = new DataProtectionCompliance();
        dataProtection.setDataBreaches(getDataBreachCount(year));
        dataProtection.setDataSubjectRequests(getDataSubjectRequests(year));
        dataProtection.setDataResidencyCompliance(
            assessDataResidencyCompliance(year)
        );
        report.setDataProtection(dataProtection);

        // Internal audit findings
        report.setInternalAuditFindings(getInternalAuditFindings(year));

        // Training and awareness
        TrainingCompliance training = new TrainingCompliance();
        training.setComplianceTrainingCompletion(
            getTrainingCompletionRate(year)
        );
        training.setAmlTrainingCompletion(
            getAmlTrainingCompletionRate(year)
        );
        report.setTrainingCompliance(training);

        // Log report generation
        auditLogger.logReportGeneration(
            "SYSTEM",
            "COMPLIANCE_REPORT",
            report.getReportId(),
            Map.of("year", year)
        );

        log.info("Compliance report generated: reportId={}", report.getReportId());

        return report;
    }

    /**
     * Generate ad-hoc regulatory report
     */
    public RegulatoryReport generateRegulatoryReport(
        String reportType, LocalDate fromDate, LocalDate toDate,
        Map<String, Object> parameters) {

        log.info("Generating regulatory report: type={}, from={}, to={}",
            reportType, fromDate, toDate);

        RegulatoryReport report = new RegulatoryReport();
        report.setReportId(UUID.randomUUID().toString());
        report.setReportType(reportType);
        report.setFromDate(fromDate);
        report.setToDate(toDate);
        report.setParameters(parameters);
        report.setGeneratedAt(LocalDateTime.now());

        // Generate report based on type
        switch (reportType) {
            case "CAPITAL_ADEQUACY":
                report.setData(generateCapitalAdequacyData(fromDate, toDate));
                break;
            case "LARGE_EXPOSURE":
                report.setData(generateLargeExposureData(fromDate, toDate));
                break;
            case "RELATED_PARTY":
                report.setData(generateRelatedPartyData(fromDate, toDate));
                break;
            case "LIQUIDITY_COVERAGE":
                report.setData(generateLiquidityCoverageData(fromDate, toDate));
                break;
            default:
                throw new IllegalArgumentException("Unknown report type: " + reportType);
        }

        // Log report generation
        auditLogger.logReportGeneration(
            "SYSTEM",
            reportType,
            report.getReportId(),
            parameters
        );

        return report;
    }

    // Helper methods

    private BigDecimal calculateTotalOutstanding(List<LoanData> loans) {
        return loans.stream()
            .map(LoanData::getOutstandingAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, List<LoanData>> classifyByDpd(List<LoanData> loans) {
        return loans.stream()
            .collect(Collectors.groupingBy(loan -> {
                int dpd = loan.getDaysPastDue();
                if (dpd == 0) return "CURRENT";
                else if (dpd <= 30) return "1-30";
                else if (dpd <= 60) return "31-60";
                else if (dpd <= 90) return "61-90";
                else if (dpd <= 180) return "91-180";
                else return "180+";
            }));
    }

    private BigDecimal calculateNplAmount(Map<String, List<LoanData>> dpdBuckets) {
        // NPL = Loans with DPD > 90 days
        BigDecimal nplAmount = BigDecimal.ZERO;

        for (Map.Entry<String, List<LoanData>> entry : dpdBuckets.entrySet()) {
            if (entry.getKey().equals("91-180") || entry.getKey().equals("180+")) {
                nplAmount = nplAmount.add(
                    calculateTotalOutstanding(entry.getValue())
                );
            }
        }

        return nplAmount;
    }

    private BigDecimal calculateNplRatio(BigDecimal nplAmount, BigDecimal totalOutstanding) {
        if (totalOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return nplAmount.divide(totalOutstanding, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    private Map<String, StageClassification> classifyByStage(List<LoanData> loans) {
        Map<String, StageClassification> stages = new HashMap<>();

        // Stage 1: Performing (DPD < 30)
        stages.put("STAGE_1", createStageClassification(loans, 0, 30));

        // Stage 2: Underperforming (DPD 30-90)
        stages.put("STAGE_2", createStageClassification(loans, 30, 90));

        // Stage 3: Non-performing (DPD > 90)
        stages.put("STAGE_3", createStageClassification(loans, 90, Integer.MAX_VALUE));

        return stages;
    }

    private StageClassification createStageClassification(
        List<LoanData> loans, int minDpd, int maxDpd) {

        List<LoanData> stageLo = loans.stream()
            .filter(loan -> loan.getDaysPastDue() >= minDpd && loan.getDaysPastDue() < maxDpd)
            .collect(Collectors.toList());

        StageClassification stage = new StageClassification();
        stage.setLoanCount(stageLoans.size());
        stage.setOutstandingAmount(calculateTotalOutstanding(stageLoans));
        stage.setProvisionAmount(calculateStageProvisions(stageLoans, minDpd));

        return stage;
    }

    private BigDecimal calculateStageProvisions(List<LoanData> loans, int minDpd) {
        // Provision rates based on stage
        BigDecimal provisionRate;
        if (minDpd == 0) {
            provisionRate = new BigDecimal("0.01"); // 1% for Stage 1
        } else if (minDpd == 30) {
            provisionRate = new BigDecimal("0.03"); // 3% for Stage 2
        } else {
            provisionRate = new BigDecimal("0.20"); // 20% for Stage 3
        }

        return calculateTotalOutstanding(loans).multiply(provisionRate);
    }

    private BigDecimal calculateProvisions(Map<String, StageClassification> stages) {
        return stages.values().stream()
            .map(StageClassification::getProvisionAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateProvisionCoverage(BigDecimal provisions, BigDecimal nplAmount) {
        if (nplAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }
        return provisions.divide(nplAmount, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    // Data provider interfaces

    public interface LoanDataProvider {
        List<LoanData> getLoansForMonth(YearMonth month);
        List<LoanData> getLoansForPeriod(LocalDate from, LocalDate to);
    }

    public interface RiskDataProvider {
        BigDecimal calculateWeightedAverageRiskGrade(int year, int quarter);
        BigDecimal calculateExpectedLoss(int year, int quarter);
        BigDecimal calculateUnexpectedLoss(int year, int quarter);
        BigDecimal calculateAveragePd(int year, int quarter);
        BigDecimal calculateAverageLgd(int year, int quarter);
        Map<String, BigDecimal> getTop10BorrowersExposure(int year, int quarter);
        Map<String, BigDecimal> getSectorConcentration(int year, int quarter);
        Map<String, BigDecimal> getGeographicConcentration(int year, int quarter);
        Map<String, BigDecimal> getProductConcentration(int year, int quarter);
        int getOperationalIncidentCount(int year, int quarter);
        BigDecimal getOperationalLoss(int year, int quarter);
        Map<String, Object> getKeyRiskIndicators(int year, int quarter);
        BigDecimal calculateLcr(int year, int quarter);
        BigDecimal calculateNsfr(int year, int quarter);
        Map<String, BigDecimal> calculateCashFlowGap(int year, int quarter);
        Map<String, Object> getStressTestResults(int year, int quarter);
    }
}