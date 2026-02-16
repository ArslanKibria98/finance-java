package com.ksa.financing.domain.sharia;

import com.ksa.financing.domain.valueobject.SarMoney;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Stateless calculator for late payment penalties that MUST be donated to charity.
 * <p>
 * <b>CRITICAL SHARIA PRINCIPLE:</b> Late payment penalties are <b>HARAM (forbidden)</b> as
 * lender income because they constitute Riba (usury/interest). Any penalty charged to discourage
 * default <b>MUST</b> be donated 100% to charity and cannot increase the lender's profit.
 * </p>
 * <p>
 * <b>Sharia Compliance (AAOIFI Sharia Standard 3, OIC Fiqh Academy Resolution):</b>
 * <ul>
 *   <li><b>Mandatory Charity:</b> 100% of penalty MUST go to legitimate Islamic charity</li>
 *   <li><b>Deterrent Only:</b> Penalty serves to discourage default, not as profit</li>
 *   <li><b>No Lender Benefit:</b> Lender cannot benefit financially from customer's default</li>
 *   <li><b>Hardship Exception:</b> Customer in genuine hardship must be given respite (Quran 2:280)</li>
 *   <li><b>Reasonableness:</b> Penalty should be reasonable, not exploitative</li>
 *   <li><b>Documentation:</b> Charity donation must be documented and auditable</li>
 *   <li><b>Intent Matters:</b> Purpose is ethical behavior, not revenue generation</li>
 * </ul>
 * </p>
 * <p>
 * <b>Quranic Guidance:</b> "If the debtor is in difficulty, grant him time till it is easy
 * for him to repay. But if you remit it by way of charity, that is best for you if you only knew."
 * (Quran 2:280)
 * </p>
 * <p>
 * <b>Formula:</b>
 * <pre>
 * Penalty Amount = Installment Amount × Daily Rate × Days Late
 * Common Daily Rate: 0.001 (0.1% per day) or 0.0005 (0.05% per day)
 * Maximum Reasonable: 50% of installment amount
 * </pre>
 * </p>
 * <p>
 * <b>Example:</b>
 * <pre>
 * Installment Amount: SAR 8,750
 * Days Late: 15 days
 * Daily Rate: 0.001 (0.1% per day)
 *
 * Penalty = 8,750 × 0.001 × 15 = SAR 131.25
 * Destination: Local poverty relief charity
 * </pre>
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public final class CharityPenaltyCalculator {

    /**
     * Default daily penalty rate: 0.1% per day (0.001 decimal).
     * This is commonly used in Islamic finance as a reasonable deterrent.
     */
    public static final BigDecimal DEFAULT_DAILY_RATE = new BigDecimal("0.001");

    /**
     * Conservative daily penalty rate: 0.05% per day (0.0005 decimal).
     * Used by more conservative institutions.
     */
    public static final BigDecimal CONSERVATIVE_DAILY_RATE = new BigDecimal("0.0005");

    /**
     * Maximum reasonable penalty as percentage of installment: 50% (0.5 decimal).
     * Penalties exceeding this may be considered exploitative.
     */
    public static final BigDecimal MAX_REASONABLE_PENALTY_RATIO = new BigDecimal("0.5");

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    // Private constructor to prevent instantiation
    private CharityPenaltyCalculator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Calculate late payment penalty for charity donation.
     * <p>
     * Uses the default daily rate (0.1% per day).
     * </p>
     *
     * @param installmentAmount   The original installment amount that was due
     * @param daysLate           The number of days the payment is late
     * @param charityType        The type/purpose of charity (e.g., "Poverty Relief")
     * @param destinationCharity The specific charity organization name
     * @return CharityDonation result
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public static CharityDonation calculate(
            SarMoney installmentAmount,
            int daysLate,
            String charityType,
            String destinationCharity
    ) {
        return calculate(installmentAmount, daysLate, DEFAULT_DAILY_RATE, charityType, destinationCharity);
    }

    /**
     * Calculate late payment penalty with custom daily rate.
     * <p>
     * Allows specifying a custom daily penalty rate. The rate should be reasonable
     * and not exploitative per Sharia principles.
     * </p>
     *
     * @param installmentAmount   The installment amount
     * @param daysLate           The days late
     * @param dailyRate          The daily penalty rate (e.g., 0.001 for 0.1% per day)
     * @param charityType        The charity type
     * @param destinationCharity The charity organization
     * @return CharityDonation result
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public static CharityDonation calculate(
            SarMoney installmentAmount,
            int daysLate,
            BigDecimal dailyRate,
            String charityType,
            String destinationCharity
    ) {
        Objects.requireNonNull(installmentAmount, "Installment amount cannot be null");
        Objects.requireNonNull(dailyRate, "Daily rate cannot be null");
        Objects.requireNonNull(charityType, "Charity type cannot be null");
        Objects.requireNonNull(destinationCharity, "Destination charity cannot be null");

        if (installmentAmount.isZero() || installmentAmount.isNegative()) {
            throw new IllegalArgumentException("Installment amount must be positive");
        }
        if (daysLate < 1) {
            throw new IllegalArgumentException("Days late must be at least 1");
        }
        if (dailyRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Daily rate must be positive");
        }
        if (dailyRate.compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalArgumentException(
                    "Daily rate exceeds reasonable limit (1% per day). Got: " + dailyRate
            );
        }
        if (charityType.isBlank()) {
            throw new IllegalArgumentException("Charity type cannot be blank");
        }
        if (destinationCharity.isBlank()) {
            throw new IllegalArgumentException("Destination charity cannot be blank");
        }

        // Calculate penalty: Installment × Daily Rate × Days
        BigDecimal penaltyMultiplier = dailyRate.multiply(BigDecimal.valueOf(daysLate));
        SarMoney penaltyAmount = installmentAmount.multiply(penaltyMultiplier);

        return new CharityDonation(
                penaltyAmount,
                charityType,
                daysLate,
                destinationCharity
        );
    }

    /**
     * Calculate penalty with cap at maximum reasonable amount (50% of installment).
     * <p>
     * Ensures penalty does not become exploitative by capping at 50% of installment.
     * </p>
     *
     * @param installmentAmount   The installment amount
     * @param daysLate           The days late
     * @param dailyRate          The daily penalty rate
     * @param charityType        The charity type
     * @param destinationCharity The charity organization
     * @return CharityDonation result with capped amount
     */
    public static CharityDonation calculateWithCap(
            SarMoney installmentAmount,
            int daysLate,
            BigDecimal dailyRate,
            String charityType,
            String destinationCharity
    ) {
        CharityDonation donation = calculate(
                installmentAmount,
                daysLate,
                dailyRate,
                charityType,
                destinationCharity
        );

        // Cap at 50% of installment
        SarMoney maxReasonable = installmentAmount.multiply(MAX_REASONABLE_PENALTY_RATIO);

        if (donation.amount().isGreaterThan(maxReasonable)) {
            return new CharityDonation(
                    maxReasonable,
                    charityType,
                    daysLate,
                    destinationCharity
            );
        }

        return donation;
    }

    /**
     * Calculate penalty amount only (without creating CharityDonation).
     *
     * @param installmentAmount The installment amount
     * @param daysLate         The days late
     * @param dailyRate        The daily penalty rate
     * @return The penalty amount
     */
    public static SarMoney calculatePenaltyAmount(
            SarMoney installmentAmount,
            int daysLate,
            BigDecimal dailyRate
    ) {
        Objects.requireNonNull(installmentAmount, "Installment amount cannot be null");
        Objects.requireNonNull(dailyRate, "Daily rate cannot be null");

        if (daysLate < 1) {
            throw new IllegalArgumentException("Days late must be at least 1");
        }

        BigDecimal penaltyMultiplier = dailyRate.multiply(BigDecimal.valueOf(daysLate));
        return installmentAmount.multiply(penaltyMultiplier);
    }

    /**
     * Calculate penalty with default rate.
     *
     * @param installmentAmount The installment amount
     * @param daysLate         The days late
     * @return The penalty amount
     */
    public static SarMoney calculatePenaltyAmount(SarMoney installmentAmount, int daysLate) {
        return calculatePenaltyAmount(installmentAmount, daysLate, DEFAULT_DAILY_RATE);
    }

    /**
     * Check if penalty amount is reasonable (not exploitative).
     * <p>
     * Returns true if penalty is less than or equal to 50% of installment amount.
     * </p>
     *
     * @param penaltyAmount     The penalty amount
     * @param installmentAmount The installment amount
     * @return true if penalty is reasonable
     */
    public static boolean isReasonablePenalty(SarMoney penaltyAmount, SarMoney installmentAmount) {
        Objects.requireNonNull(penaltyAmount, "Penalty amount cannot be null");
        Objects.requireNonNull(installmentAmount, "Installment amount cannot be null");

        SarMoney maxReasonable = installmentAmount.multiply(MAX_REASONABLE_PENALTY_RATIO);
        return !penaltyAmount.isGreaterThan(maxReasonable);
    }

    /**
     * Calculate the effective daily rate from penalty amount.
     * <p>
     * Reverse calculation: given penalty and days late, determine the rate used.
     * </p>
     *
     * @param penaltyAmount     The penalty amount
     * @param installmentAmount The installment amount
     * @param daysLate         The days late
     * @return The effective daily rate as decimal
     */
    public static BigDecimal calculateEffectiveDailyRate(
            SarMoney penaltyAmount,
            SarMoney installmentAmount,
            int daysLate
    ) {
        Objects.requireNonNull(penaltyAmount, "Penalty amount cannot be null");
        Objects.requireNonNull(installmentAmount, "Installment amount cannot be null");

        if (daysLate < 1) {
            throw new IllegalArgumentException("Days late must be at least 1");
        }
        if (installmentAmount.isZero()) {
            return BigDecimal.ZERO;
        }

        // Rate = Penalty / (Installment × Days)
        BigDecimal penaltyRatio = penaltyAmount.getValue()
                .divide(installmentAmount.getValue(), 6, ROUNDING_MODE);
        return penaltyRatio.divide(BigDecimal.valueOf(daysLate), 6, ROUNDING_MODE);
    }

    /**
     * Calculate total penalty for multiple late installments.
     * <p>
     * Calculates individual penalties and aggregates them.
     * </p>
     *
     * @param installmentAmount   The installment amount (assuming equal installments)
     * @param daysLatePerInstallment Array of days late for each installment
     * @param dailyRate          The daily penalty rate
     * @param charityType        The charity type
     * @param destinationCharity The charity organization
     * @return Aggregated CharityDonation result
     */
    public static CharityDonation calculateForMultipleInstallments(
            SarMoney installmentAmount,
            int[] daysLatePerInstallment,
            BigDecimal dailyRate,
            String charityType,
            String destinationCharity
    ) {
        Objects.requireNonNull(installmentAmount, "Installment amount cannot be null");
        Objects.requireNonNull(daysLatePerInstallment, "Days late array cannot be null");

        if (daysLatePerInstallment.length == 0) {
            throw new IllegalArgumentException("Days late array cannot be empty");
        }

        SarMoney totalPenalty = SarMoney.zero();
        int maxDaysLate = 0;

        for (int daysLate : daysLatePerInstallment) {
            if (daysLate > 0) {
                SarMoney penalty = calculatePenaltyAmount(installmentAmount, daysLate, dailyRate);
                totalPenalty = totalPenalty.add(penalty);
                maxDaysLate = Math.max(maxDaysLate, daysLate);
            }
        }

        if (totalPenalty.isZero()) {
            throw new IllegalArgumentException("No late installments found");
        }

        return new CharityDonation(
                totalPenalty,
                charityType,
                maxDaysLate,
                destinationCharity
        );
    }

    /**
     * Validate if daily rate is within reasonable bounds for Sharia compliance.
     *
     * @param dailyRate The daily penalty rate
     * @return true if rate is reasonable (between 0.01% and 1% per day)
     */
    public static boolean isReasonableDailyRate(BigDecimal dailyRate) {
        if (dailyRate == null) {
            return false;
        }
        BigDecimal minRate = new BigDecimal("0.0001"); // 0.01% per day
        BigDecimal maxRate = new BigDecimal("0.01");   // 1% per day
        return dailyRate.compareTo(minRate) >= 0 && dailyRate.compareTo(maxRate) <= 0;
    }

    /**
     * Get recommended daily rate based on jurisdiction/practice.
     *
     * @param conservative If true, returns more conservative rate (0.05% per day)
     * @return Recommended daily rate
     */
    public static BigDecimal getRecommendedDailyRate(boolean conservative) {
        return conservative ? CONSERVATIVE_DAILY_RATE : DEFAULT_DAILY_RATE;
    }
}
