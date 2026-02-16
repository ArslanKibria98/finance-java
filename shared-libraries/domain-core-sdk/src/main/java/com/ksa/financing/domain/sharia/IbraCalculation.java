package com.ksa.financing.domain.sharia;

import com.ksa.financing.domain.valueobject.SarMoney;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Result record containing Ibra (rebate/waiver) calculation for early settlement.
 * <p>
 * <b>Ibra Overview:</b> Ibra is a voluntary waiver or reduction of debt by the creditor.
 * In Islamic financing, when a customer wishes to settle early, the financier may grant
 * Ibra by waiving the unearned profit portion of the remaining debt.
 * </p>
 * <p>
 * <b>Sharia Compliance (AAOIFI Sharia Standards 2, 8, 9):</b>
 * <ul>
 *   <li>Ibra (waiver) MUST be voluntary by the financier</li>
 *   <li>Customer CANNOT demand or be promised Ibra at contract inception</li>
 *   <li>Only unearned profit may be waived; earned profit is legitimately due</li>
 *   <li>Time-proportionate calculation: Earned Profit = Total Profit × (Time Elapsed / Total Time)</li>
 *   <li>Settlement Amount = Outstanding Principal + Earned Profit only</li>
 *   <li>Waiving profit is an act of virtue (recommended, not obligatory)</li>
 *   <li>Pre-agreed penalty for early settlement is NOT permissible</li>
 * </ul>
 * </p>
 * <p>
 * <b>Formula:</b>
 * <pre>
 * Earned Profit = Total Profit × (Months Elapsed / Total Months)
 * Unearned Profit = Total Profit - Earned Profit
 * Waived Profit (Ibra) = Unearned Profit (or a portion thereof)
 * Settlement Amount = Outstanding Principal + Earned Profit - Waived Amount
 * </pre>
 * </p>
 *
 * @param settlementDate       The date of early settlement
 * @param outstandingPrincipal The principal amount still owed
 * @param earnedProfit         The profit earned up to settlement date (time-proportionate)
 * @param waivedProfit         The unearned profit being waived (Ibra)
 * @param settlementAmount     The total amount due for settlement
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record IbraCalculation(
        LocalDate settlementDate,
        SarMoney outstandingPrincipal,
        SarMoney earnedProfit,
        SarMoney waivedProfit,
        SarMoney settlementAmount
) {
    /**
     * Canonical constructor with validation.
     */
    public IbraCalculation {
        Objects.requireNonNull(settlementDate, "Settlement date cannot be null");
        Objects.requireNonNull(outstandingPrincipal, "Outstanding principal cannot be null");
        Objects.requireNonNull(earnedProfit, "Earned profit cannot be null");
        Objects.requireNonNull(waivedProfit, "Waived profit cannot be null");
        Objects.requireNonNull(settlementAmount, "Settlement amount cannot be null");

        // Validate all amounts are non-negative
        if (outstandingPrincipal.isNegative()) {
            throw new IllegalArgumentException("Outstanding principal cannot be negative");
        }
        if (earnedProfit.isNegative()) {
            throw new IllegalArgumentException("Earned profit cannot be negative");
        }
        if (waivedProfit.isNegative()) {
            throw new IllegalArgumentException("Waived profit cannot be negative");
        }
        if (settlementAmount.isNegative()) {
            throw new IllegalArgumentException("Settlement amount cannot be negative");
        }

        // Validate settlement amount calculation
        SarMoney calculatedSettlement = outstandingPrincipal.add(earnedProfit).subtract(waivedProfit);
        if (!calculatedSettlement.equals(settlementAmount)) {
            throw new IllegalArgumentException(
                    String.format("Settlement amount must equal principal + earned profit - waived profit. " +
                            "Expected: %s, Got: %s", calculatedSettlement, settlementAmount)
            );
        }
    }

    /**
     * Get the total amount that would be due without Ibra (before waiver).
     *
     * @return amount without waiver
     */
    public SarMoney getAmountWithoutIbra() {
        return outstandingPrincipal.add(earnedProfit);
    }

    /**
     * Calculate the customer savings from Ibra.
     *
     * @return the amount saved due to waiver
     */
    public SarMoney getCustomerSavings() {
        return waivedProfit;
    }

    /**
     * Calculate the Ibra percentage (what % of earned profit was waived).
     *
     * @return waiver percentage as decimal
     */
    public double getIbraPercentage() {
        SarMoney totalDue = getAmountWithoutIbra();
        if (totalDue.isZero()) {
            return 0.0;
        }
        return waivedProfit.getValue()
                .divide(totalDue.getValue(), 4, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Check if any Ibra (waiver) was granted.
     *
     * @return true if profit was waived
     */
    public boolean hasIbra() {
        return waivedProfit.isPositive();
    }

    /**
     * Get a description of the Ibra for customer communication.
     *
     * @return formatted description
     */
    public String getIbraDescription() {
        if (!hasIbra()) {
            return "No Ibra (rebate) granted. Full earned profit is due.";
        }
        return String.format("Ibra (Rebate) granted: %s waived (%.2f%% of amount due). " +
                        "This is a voluntary act of generosity by the financier.",
                waivedProfit.toFormattedString(),
                getIbraPercentage() * 100
        );
    }

    /**
     * Calculate the discount rate given to customer (waived / original amount).
     *
     * @return discount rate as decimal
     */
    public double getEffectiveDiscountRate() {
        return getIbraPercentage();
    }

    /**
     * Get settlement breakdown for customer statement.
     *
     * @return formatted breakdown
     */
    public String getSettlementBreakdown() {
        return String.format("""
                Early Settlement Breakdown (as of %s):
                ----------------------------------------
                Outstanding Principal:    %s
                Earned Profit:           %s
                ----------------------------------------
                Subtotal:                %s
                Less: Ibra (Rebate):     (%s)
                ----------------------------------------
                Settlement Amount:       %s

                %s
                """,
                settlementDate,
                outstandingPrincipal.toFormattedString(),
                earnedProfit.toFormattedString(),
                getAmountWithoutIbra().toFormattedString(),
                waivedProfit.toFormattedString(),
                settlementAmount.toFormattedString(),
                getIbraDescription()
        );
    }

    @Override
    public String toString() {
        return String.format("IbraCalculation[Date: %s, Principal: %s, Earned Profit: %s, Waived: %s, Settlement: %s]",
                settlementDate,
                outstandingPrincipal.toFormattedString(),
                earnedProfit.toFormattedString(),
                waivedProfit.toFormattedString(),
                settlementAmount.toFormattedString()
        );
    }
}
