package com.ksa.financing.domain.sharia;

import com.ksa.financing.domain.valueobject.SarMoney;

import java.util.List;
import java.util.Objects;

/**
 * Result record containing complete Murabaha financing calculations.
 * <p>
 * <b>Murabaha Overview:</b> Murabaha is a cost-plus-profit sale contract where the financier
 * purchases an asset and sells it to the customer at cost plus an agreed markup. The sale price
 * and profit are disclosed and agreed upon at contract inception.
 * </p>
 * <p>
 * <b>Sharia Compliance (AAOIFI FAS 2):</b>
 * <ul>
 *   <li>Cost price must be disclosed to customer</li>
 *   <li>Profit markup must be known and agreed upon</li>
 *   <li>Sale price = Cost + Profit (predetermined)</li>
 *   <li>Financier must own asset before selling to customer</li>
 *   <li>Asset must be lawful (halal) and exist at time of sale</li>
 *   <li>Payment can be deferred (installments allowed)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Formula:</b>
 * <pre>
 * Profit Amount = Cost Price × Profit Rate
 * Sale Price = Cost Price + Profit Amount
 * Monthly Installment = Sale Price ÷ Tenure (months)
 * </pre>
 * </p>
 *
 * @param costPrice          The original cost of the asset to the financier (must be disclosed)
 * @param profitAmount       The agreed-upon profit markup
 * @param salePrice          The total sale price (cost + profit)
 * @param monthlyInstallment The fixed monthly payment amount
 * @param schedule           Detailed amortization schedule showing principal and profit breakdown
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record MurabahaCalculation(
        SarMoney costPrice,
        SarMoney profitAmount,
        SarMoney salePrice,
        SarMoney monthlyInstallment,
        List<InstallmentLine> schedule
) {
    /**
     * Canonical constructor with validation.
     */
    public MurabahaCalculation {
        Objects.requireNonNull(costPrice, "Cost price cannot be null");
        Objects.requireNonNull(profitAmount, "Profit amount cannot be null");
        Objects.requireNonNull(salePrice, "Sale price cannot be null");
        Objects.requireNonNull(monthlyInstallment, "Monthly installment cannot be null");
        Objects.requireNonNull(schedule, "Schedule cannot be null");

        // Validate that sale price equals cost + profit
        SarMoney calculatedSalePrice = costPrice.add(profitAmount);
        if (!calculatedSalePrice.equals(salePrice)) {
            throw new IllegalArgumentException(
                    String.format("Sale price must equal cost price + profit amount. " +
                            "Expected: %s, Got: %s", calculatedSalePrice, salePrice)
            );
        }

        // Validate all amounts are non-negative
        if (costPrice.isNegative()) {
            throw new IllegalArgumentException("Cost price cannot be negative");
        }
        if (profitAmount.isNegative()) {
            throw new IllegalArgumentException("Profit amount cannot be negative");
        }
        if (monthlyInstallment.isNegative()) {
            throw new IllegalArgumentException("Monthly installment cannot be negative");
        }

        // Validate schedule is not empty
        if (schedule.isEmpty()) {
            throw new IllegalArgumentException("Schedule cannot be empty");
        }

        // Make schedule immutable
        schedule = List.copyOf(schedule);
    }

    /**
     * Get the total number of installments.
     *
     * @return the number of installments
     */
    public int getTotalInstallments() {
        return schedule.size();
    }

    /**
     * Get the profit rate as a decimal (profit / cost).
     *
     * @return the effective profit rate
     */
    public double getEffectiveProfitRate() {
        if (costPrice.isZero()) {
            return 0.0;
        }
        return profitAmount.getValue()
                .divide(costPrice.getValue(), 4, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Get the total amount to be paid (sum of all installments).
     *
     * @return the total repayment amount
     */
    public SarMoney getTotalRepaymentAmount() {
        return schedule.stream()
                .map(InstallmentLine::totalInstallment)
                .reduce(SarMoney.zero(), SarMoney::add);
    }

    /**
     * Get a specific installment by number.
     *
     * @param installmentNumber the installment number (1-based)
     * @return the installment line
     * @throws IllegalArgumentException if installment number is invalid
     */
    public InstallmentLine getInstallment(int installmentNumber) {
        if (installmentNumber < 1 || installmentNumber > schedule.size()) {
            throw new IllegalArgumentException(
                    String.format("Invalid installment number: %d. Valid range: 1-%d",
                            installmentNumber, schedule.size())
            );
        }
        return schedule.get(installmentNumber - 1);
    }

    @Override
    public String toString() {
        return String.format("MurabahaCalculation[Cost: %s, Profit: %s, Sale: %s, Monthly: %s, Installments: %d]",
                costPrice.toFormattedString(),
                profitAmount.toFormattedString(),
                salePrice.toFormattedString(),
                monthlyInstallment.toFormattedString(),
                getTotalInstallments()
        );
    }
}
