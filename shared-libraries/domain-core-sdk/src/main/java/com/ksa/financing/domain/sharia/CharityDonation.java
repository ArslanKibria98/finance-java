package com.ksa.financing.domain.sharia;

import com.ksa.financing.domain.valueobject.SarMoney;

import java.util.Objects;

/**
 * Result record representing a charity donation from late payment penalties.
 * <p>
 * <b>Sharia Principle:</b> Late payment penalties are <b>HARAM (forbidden) as lender income</b>
 * because they constitute Riba (usury). However, penalties may be charged to discourage
 * default, with the <b>MANDATORY requirement</b> that 100% goes to charity.
 * </p>
 * <p>
 * <b>Sharia Compliance (AAOIFI Sharia Standard 3):</b>
 * <ul>
 *   <li>Penalty amount MUST NOT increase lender's profit</li>
 *   <li>100% of penalty MUST be donated to legitimate charity</li>
 *   <li>Penalty serves only as deterrent against default, not profit source</li>
 *   <li>Customer in genuine hardship should be given respite (Quran 2:280)</li>
 *   <li>Penalty should be reasonable, not exploitative</li>
 *   <li>Lender cannot benefit financially from customer's default</li>
 * </ul>
 * </p>
 * <p>
 * <b>Formula:</b>
 * <pre>
 * Penalty Amount = Installment Amount × Daily Rate × Days Late
 * Common Daily Rate: 0.001 (0.1% per day)
 * </pre>
 * </p>
 * <p>
 * <b>Charity Types:</b> Zakat-eligible charities, Islamic educational institutions,
 * poverty relief organizations, medical aid, etc.
 * </p>
 *
 * @param amount             The penalty amount to be donated to charity
 * @param charityType        The category/purpose of charity (e.g., "Poverty Relief", "Education")
 * @param daysLate           The number of days the payment was late
 * @param destinationCharity The specific charity organization to receive the donation
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record CharityDonation(
        SarMoney amount,
        String charityType,
        int daysLate,
        String destinationCharity
) {
    /**
     * Canonical constructor with validation.
     */
    public CharityDonation {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(charityType, "Charity type cannot be null");
        Objects.requireNonNull(destinationCharity, "Destination charity cannot be null");

        if (charityType.isBlank()) {
            throw new IllegalArgumentException("Charity type cannot be blank");
        }
        if (destinationCharity.isBlank()) {
            throw new IllegalArgumentException("Destination charity cannot be blank");
        }
        if (daysLate < 1) {
            throw new IllegalArgumentException("Days late must be at least 1");
        }
        if (amount.isNegative()) {
            throw new IllegalArgumentException("Penalty amount cannot be negative");
        }
        if (amount.isZero()) {
            throw new IllegalArgumentException("Penalty amount must be greater than zero");
        }
    }

    /**
     * Calculate the daily penalty rate that was applied.
     *
     * @param installmentAmount the original installment amount
     * @return the daily rate as a decimal
     */
    public double calculateDailyRate(SarMoney installmentAmount) {
        Objects.requireNonNull(installmentAmount, "Installment amount cannot be null");
        if (installmentAmount.isZero()) {
            return 0.0;
        }
        return amount.getValue()
                .divide(installmentAmount.getValue(), 6, java.math.RoundingMode.HALF_UP)
                .divide(java.math.BigDecimal.valueOf(daysLate), 6, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Check if the penalty amount is reasonable (not exploitative).
     * Uses 50% of original installment as maximum reasonable penalty.
     *
     * @param installmentAmount the original installment amount
     * @return true if penalty is within reasonable limits
     */
    public boolean isReasonable(SarMoney installmentAmount) {
        Objects.requireNonNull(installmentAmount, "Installment amount cannot be null");
        // Maximum reasonable penalty: 50% of installment amount
        SarMoney maxReasonable = installmentAmount.multiply(0.5);
        return amount.isLessThan(maxReasonable) || amount.equals(maxReasonable);
    }

    /**
     * Get a description of the charity donation for audit trails.
     *
     * @return formatted description
     */
    public String getDescription() {
        return String.format("Late payment penalty (%d days late) donated to %s (%s): %s",
                daysLate,
                destinationCharity,
                charityType,
                amount.toFormattedString()
        );
    }

    /**
     * Create a warning message if penalty appears exploitative.
     *
     * @param installmentAmount the original installment amount
     * @return warning message or null if reasonable
     */
    public String getExploitationWarning(SarMoney installmentAmount) {
        if (isReasonable(installmentAmount)) {
            return null;
        }
        return String.format("WARNING: Penalty amount (%s) exceeds 50%% of installment amount (%s). " +
                        "This may be considered exploitative under Sharia principles. " +
                        "Review per AAOIFI Sharia Standard 3.",
                amount.toFormattedString(),
                installmentAmount.toFormattedString()
        );
    }

    @Override
    public String toString() {
        return String.format("CharityDonation[Amount: %s, Type: %s, Days Late: %d, Charity: %s]",
                amount.toFormattedString(),
                charityType,
                daysLate,
                destinationCharity
        );
    }
}
