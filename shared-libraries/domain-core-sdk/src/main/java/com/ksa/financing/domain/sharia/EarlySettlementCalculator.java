package com.ksa.financing.domain.sharia;

import com.ksa.financing.domain.valueobject.SarMoney;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * Stateless calculator for early settlement with Ibra (rebate/waiver) calculations.
 * <p>
 * <b>Ibra Definition:</b> Ibra is a voluntary waiver or reduction of debt by the creditor.
 * In Islamic financing, when a customer wishes to settle a contract early, the financier
 * may grant Ibra by waiving some or all of the unearned profit portion.
 * </p>
 * <p>
 * <b>Sharia Compliance Requirements (AAOIFI Sharia Standards 2, 8, 9):</b>
 * <ul>
 *   <li><b>Voluntary:</b> Ibra MUST be voluntary by the financier, not demanded by customer</li>
 *   <li><b>No Pre-Agreement:</b> Cannot promise Ibra at contract inception (would make it conditional sale)</li>
 *   <li><b>Time-Proportionate:</b> Earned profit calculated based on time elapsed</li>
 *   <li><b>Unearned Profit:</b> Only unearned (future) profit may be waived</li>
 *   <li><b>Settlement Amount:</b> Principal + Earned Profit (unearned profit waived)</li>
 *   <li><b>Virtuous Act:</b> Granting Ibra is recommended (mustahabb) but not obligatory</li>
 *   <li><b>No Early Settlement Penalty:</b> Charging extra for early settlement is forbidden</li>
 * </ul>
 * </p>
 * <p>
 * <b>Key Principle:</b> The financier is entitled to profit only for the actual period
 * of financing. Future profit (after early settlement) was never earned and should not
 * be claimed. This aligns with the Islamic principle that profit must be tied to risk
 * and actual service rendered.
 * </p>
 * <p>
 * <b>Calculation Method:</b>
 * <pre>
 * Time Elapsed = Settlement Date - Contract Start Date
 * Total Contract Period = Contract End Date - Contract Start Date
 * Earned Profit Ratio = Time Elapsed / Total Contract Period
 * Earned Profit = Total Profit × Earned Profit Ratio
 * Unearned Profit = Total Profit - Earned Profit
 * Waived Profit (Ibra) = Unearned Profit (or a portion thereof)
 * Settlement Amount = Outstanding Principal + Earned Profit - Waived Profit
 * </pre>
 * </p>
 * <p>
 * <b>Example:</b>
 * <pre>
 * Contract Details:
 * - Principal: SAR 100,000
 * - Total Profit: SAR 5,000
 * - Contract Period: 12 months
 * - Monthly Payment: SAR 8,750
 * - Start Date: 2024-01-01
 * - Settlement Date: 2024-07-01 (6 months into contract)
 *
 * Calculation:
 * - Time Elapsed: 6 months
 * - Earned Profit Ratio: 6/12 = 50%
 * - Earned Profit: SAR 5,000 × 50% = SAR 2,500
 * - Payments Made: 6 × SAR 8,750 = SAR 52,500
 * - Principal Paid: SAR 52,500 - actual profit paid = ~SAR 50,000
 * - Outstanding Principal: SAR 50,000
 * - Unearned Profit: SAR 5,000 - SAR 2,500 = SAR 2,500
 * - Waived (Ibra): SAR 2,500 (100% waiver - most generous)
 * - Settlement: SAR 50,000 + SAR 2,500 = SAR 52,500
 * </pre>
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public final class EarlySettlementCalculator {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    // Private constructor to prevent instantiation
    private EarlySettlementCalculator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Calculate early settlement with full Ibra (100% waiver of unearned profit).
     * <p>
     * This is the most generous approach and is recommended in Sharia as an act of virtue.
     * Customer pays only: Outstanding Principal + Earned Profit.
     * </p>
     *
     * @param principal         The original principal amount
     * @param totalProfit       The total profit agreed in contract
     * @param schedule          The amortization schedule
     * @param contractStartDate The contract start date
     * @param settlementDate    The early settlement date
     * @return IbraCalculation with full waiver
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public static IbraCalculation calculateWithFullIbra(
            SarMoney principal,
            SarMoney totalProfit,
            List<InstallmentLine> schedule,
            LocalDate contractStartDate,
            LocalDate settlementDate
    ) {
        return calculate(
                principal,
                totalProfit,
                schedule,
                contractStartDate,
                settlementDate,
                BigDecimal.ONE // 100% waiver
        );
    }

    /**
     * Calculate early settlement with partial Ibra (partial waiver of unearned profit).
     * <p>
     * Financier may choose to waive a portion of unearned profit based on policy,
     * customer relationship, or business considerations.
     * </p>
     *
     * @param principal         The original principal amount
     * @param totalProfit       The total profit agreed in contract
     * @param schedule          The amortization schedule
     * @param contractStartDate The contract start date
     * @param settlementDate    The early settlement date
     * @param ibraPercentage    The percentage of unearned profit to waive (0.0 to 1.0)
     * @return IbraCalculation with partial waiver
     */
    public static IbraCalculation calculate(
            SarMoney principal,
            SarMoney totalProfit,
            List<InstallmentLine> schedule,
            LocalDate contractStartDate,
            LocalDate settlementDate,
            BigDecimal ibraPercentage
    ) {
        Objects.requireNonNull(principal, "Principal cannot be null");
        Objects.requireNonNull(totalProfit, "Total profit cannot be null");
        Objects.requireNonNull(schedule, "Schedule cannot be null");
        Objects.requireNonNull(contractStartDate, "Contract start date cannot be null");
        Objects.requireNonNull(settlementDate, "Settlement date cannot be null");
        Objects.requireNonNull(ibraPercentage, "Ibra percentage cannot be null");

        if (schedule.isEmpty()) {
            throw new IllegalArgumentException("Schedule cannot be empty");
        }
        if (settlementDate.isBefore(contractStartDate)) {
            throw new IllegalArgumentException("Settlement date cannot be before contract start date");
        }
        if (ibraPercentage.compareTo(BigDecimal.ZERO) < 0 || ibraPercentage.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Ibra percentage must be between 0 and 1");
        }

        // Get contract end date from schedule
        LocalDate contractEndDate = schedule.get(schedule.size() - 1).dueDate();

        if (settlementDate.isAfter(contractEndDate)) {
            throw new IllegalArgumentException("Settlement date is after contract end date");
        }

        // Calculate time proportions
        long totalDays = ChronoUnit.DAYS.between(contractStartDate, contractEndDate);
        long elapsedDays = ChronoUnit.DAYS.between(contractStartDate, settlementDate);

        if (totalDays <= 0) {
            throw new IllegalArgumentException("Invalid contract period");
        }

        BigDecimal earnedRatio = BigDecimal.valueOf(elapsedDays)
                .divide(BigDecimal.valueOf(totalDays), 6, ROUNDING_MODE);

        // Calculate earned profit (time-proportionate)
        SarMoney earnedProfit = totalProfit.multiply(earnedRatio);

        // Calculate unearned profit
        SarMoney unearnedProfit = totalProfit.subtract(earnedProfit);

        // Calculate waived amount (Ibra)
        SarMoney waivedProfit = unearnedProfit.multiply(ibraPercentage);

        // Calculate outstanding principal at settlement date
        SarMoney outstandingPrincipal = calculateOutstandingPrincipal(schedule, settlementDate);

        // Calculate settlement amount
        SarMoney settlementAmount = outstandingPrincipal.add(earnedProfit).subtract(waivedProfit);

        return new IbraCalculation(
                settlementDate,
                outstandingPrincipal,
                earnedProfit,
                waivedProfit,
                settlementAmount
        );
    }

    /**
     * Calculate early settlement without Ibra (no waiver).
     * <p>
     * Customer pays full outstanding balance including all remaining profit.
     * While permissible, this is less generous and not encouraged in Sharia.
     * </p>
     *
     * @param principal         The original principal amount
     * @param totalProfit       The total profit
     * @param schedule          The schedule
     * @param contractStartDate The contract start date
     * @param settlementDate    The settlement date
     * @return IbraCalculation with no waiver
     */
    public static IbraCalculation calculateWithoutIbra(
            SarMoney principal,
            SarMoney totalProfit,
            List<InstallmentLine> schedule,
            LocalDate contractStartDate,
            LocalDate settlementDate
    ) {
        return calculate(
                principal,
                totalProfit,
                schedule,
                contractStartDate,
                settlementDate,
                BigDecimal.ZERO // No waiver
        );
    }

    /**
     * Calculate early settlement from Murabaha calculation.
     * <p>
     * Convenience method that extracts necessary data from MurabahaCalculation.
     * </p>
     *
     * @param murabahaCalculation The Murabaha calculation
     * @param contractStartDate   The contract start date
     * @param settlementDate      The settlement date
     * @param ibraPercentage      The Ibra percentage (0.0 to 1.0)
     * @return IbraCalculation result
     */
    public static IbraCalculation calculateFromMurabaha(
            MurabahaCalculation murabahaCalculation,
            LocalDate contractStartDate,
            LocalDate settlementDate,
            BigDecimal ibraPercentage
    ) {
        Objects.requireNonNull(murabahaCalculation, "Murabaha calculation cannot be null");

        return calculate(
                murabahaCalculation.costPrice(),
                murabahaCalculation.profitAmount(),
                murabahaCalculation.schedule(),
                contractStartDate,
                settlementDate,
                ibraPercentage
        );
    }

    /**
     * Calculate outstanding principal at a given date from schedule.
     * <p>
     * Sums up all principal payments due before or on the settlement date
     * and subtracts from total principal.
     * </p>
     *
     * @param schedule       The amortization schedule
     * @param settlementDate The settlement date
     * @return Outstanding principal amount
     */
    public static SarMoney calculateOutstandingPrincipal(
            List<InstallmentLine> schedule,
            LocalDate settlementDate
    ) {
        Objects.requireNonNull(schedule, "Schedule cannot be null");
        Objects.requireNonNull(settlementDate, "Settlement date cannot be null");

        if (schedule.isEmpty()) {
            throw new IllegalArgumentException("Schedule cannot be empty");
        }

        // Find the last installment due on or before settlement date
        InstallmentLine relevantInstallment = null;
        for (InstallmentLine line : schedule) {
            if (line.dueDate().isAfter(settlementDate)) {
                break;
            }
            relevantInstallment = line;
        }

        // If settlement is before first installment, outstanding = opening principal
        if (relevantInstallment == null) {
            return schedule.get(0).openingPrincipal();
        }

        // Otherwise, outstanding = closing principal of the last completed installment
        return relevantInstallment.closingPrincipal();
    }

    /**
     * Calculate earned profit based on time elapsed.
     * <p>
     * Time-proportionate calculation: Total Profit × (Days Elapsed / Total Days)
     * </p>
     *
     * @param totalProfit       The total profit
     * @param contractStartDate The contract start date
     * @param contractEndDate   The contract end date
     * @param settlementDate    The settlement date
     * @return Earned profit amount
     */
    public static SarMoney calculateEarnedProfit(
            SarMoney totalProfit,
            LocalDate contractStartDate,
            LocalDate contractEndDate,
            LocalDate settlementDate
    ) {
        Objects.requireNonNull(totalProfit, "Total profit cannot be null");
        Objects.requireNonNull(contractStartDate, "Contract start date cannot be null");
        Objects.requireNonNull(contractEndDate, "Contract end date cannot be null");
        Objects.requireNonNull(settlementDate, "Settlement date cannot be null");

        long totalDays = ChronoUnit.DAYS.between(contractStartDate, contractEndDate);
        long elapsedDays = ChronoUnit.DAYS.between(contractStartDate, settlementDate);

        if (totalDays <= 0) {
            throw new IllegalArgumentException("Invalid contract period");
        }
        if (elapsedDays < 0) {
            throw new IllegalArgumentException("Settlement date before contract start");
        }
        if (elapsedDays > totalDays) {
            return totalProfit; // All profit earned if past contract end
        }

        BigDecimal earnedRatio = BigDecimal.valueOf(elapsedDays)
                .divide(BigDecimal.valueOf(totalDays), 6, ROUNDING_MODE);

        return totalProfit.multiply(earnedRatio);
    }

    /**
     * Calculate unearned profit at settlement date.
     *
     * @param totalProfit    The total profit
     * @param earnedProfit   The earned profit
     * @return Unearned profit amount
     */
    public static SarMoney calculateUnearnedProfit(SarMoney totalProfit, SarMoney earnedProfit) {
        Objects.requireNonNull(totalProfit, "Total profit cannot be null");
        Objects.requireNonNull(earnedProfit, "Earned profit cannot be null");
        return totalProfit.subtract(earnedProfit);
    }

    /**
     * Calculate customer savings from Ibra.
     *
     * @param unearnedProfit The unearned profit
     * @param ibraPercentage The Ibra percentage
     * @return Amount saved by customer
     */
    public static SarMoney calculateCustomerSavings(
            SarMoney unearnedProfit,
            BigDecimal ibraPercentage
    ) {
        Objects.requireNonNull(unearnedProfit, "Unearned profit cannot be null");
        Objects.requireNonNull(ibraPercentage, "Ibra percentage cannot be null");
        return unearnedProfit.multiply(ibraPercentage);
    }

    /**
     * Determine recommended Ibra percentage based on time elapsed.
     * <p>
     * Industry practice: Higher Ibra percentage for longer elapsed period,
     * as less benefit received by customer.
     * </p>
     *
     * @param timeElapsedRatio Time elapsed / Total time (0.0 to 1.0)
     * @return Recommended Ibra percentage
     */
    public static BigDecimal getRecommendedIbraPercentage(BigDecimal timeElapsedRatio) {
        Objects.requireNonNull(timeElapsedRatio, "Time elapsed ratio cannot be null");

        if (timeElapsedRatio.compareTo(BigDecimal.ZERO) < 0 ||
            timeElapsedRatio.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Time elapsed ratio must be between 0 and 1");
        }

        // Recommendation: 100% Ibra throughout (most generous)
        // Some institutions use tiered approach:
        // < 25% elapsed: 100% waiver
        // 25-50%: 80% waiver
        // 50-75%: 60% waiver
        // > 75%: 40% waiver

        if (timeElapsedRatio.compareTo(new BigDecimal("0.25")) < 0) {
            return BigDecimal.ONE; // 100%
        } else if (timeElapsedRatio.compareTo(new BigDecimal("0.50")) < 0) {
            return new BigDecimal("0.80"); // 80%
        } else if (timeElapsedRatio.compareTo(new BigDecimal("0.75")) < 0) {
            return new BigDecimal("0.60"); // 60%
        } else {
            return new BigDecimal("0.40"); // 40%
        }
    }

    /**
     * Validate if early settlement date is valid.
     *
     * @param settlementDate    The settlement date
     * @param contractStartDate The contract start date
     * @param contractEndDate   The contract end date
     * @return true if valid
     */
    public static boolean isValidSettlementDate(
            LocalDate settlementDate,
            LocalDate contractStartDate,
            LocalDate contractEndDate
    ) {
        return settlementDate != null
                && contractStartDate != null
                && contractEndDate != null
                && !settlementDate.isBefore(contractStartDate)
                && settlementDate.isBefore(contractEndDate);
    }
}
