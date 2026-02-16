package com.ksa.financing.domain.sharia;

import com.ksa.financing.domain.valueobject.SarMoney;

import java.util.List;
import java.util.Objects;

/**
 * Result record containing complete Tawarruq financing calculations.
 * <p>
 * <b>Tawarruq Overview:</b> Tawarruq (Monetization) is a three-party financing structure where:
 * <ol>
 *   <li>Financier purchases a commodity from Supplier A (at cost price)</li>
 *   <li>Financier sells the commodity to Customer on deferred payment (cost + profit)</li>
 *   <li>Customer immediately sells the commodity to Buyer B for cash (at/near cost price)</li>
 *   <li>Customer receives liquidity while owing the financier the deferred sale price</li>
 * </ol>
 * </p>
 * <p>
 * <b>Sharia Compliance (AAOIFI Sharia Standard 30):</b>
 * <ul>
 *   <li>Commodity must be real, lawful (halal), and permissible to trade</li>
 *   <li>All three sales must be genuine arm's-length transactions</li>
 *   <li>Financier must take ownership and bear risk before selling to customer</li>
 *   <li>Customer must own commodity before selling to third party</li>
 *   <li>Commodity cannot be gold, silver, or currencies (to avoid Riba al-Fadl)</li>
 *   <li>Common commodities: metals (copper, aluminum), energy (crude oil)</li>
 *   <li>Customer cannot be obligated to sell to a specific party</li>
 *   <li><b>Controversial:</b> Some scholars discourage "organized Tawarruq" if it's engineered solely for liquidity</li>
 * </ul>
 * </p>
 * <p>
 * <b>Calculation:</b> Identical to Murabaha (cost + profit), but includes commodity transaction tracking.
 * </p>
 *
 * @param costPrice              The cost price of the commodity
 * @param profitAmount           The profit markup
 * @param salePrice              The total sale price to customer
 * @param monthlyInstallment     The fixed monthly payment
 * @param schedule               Detailed amortization schedule
 * @param commodityTransactionId The reference ID for the commodity purchase/sale chain
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record TawarruqCalculation(
        SarMoney costPrice,
        SarMoney profitAmount,
        SarMoney salePrice,
        SarMoney monthlyInstallment,
        List<InstallmentLine> schedule,
        String commodityTransactionId
) {
    /**
     * Canonical constructor with validation.
     */
    public TawarruqCalculation {
        Objects.requireNonNull(costPrice, "Cost price cannot be null");
        Objects.requireNonNull(profitAmount, "Profit amount cannot be null");
        Objects.requireNonNull(salePrice, "Sale price cannot be null");
        Objects.requireNonNull(monthlyInstallment, "Monthly installment cannot be null");
        Objects.requireNonNull(schedule, "Schedule cannot be null");
        Objects.requireNonNull(commodityTransactionId, "Commodity transaction ID cannot be null");

        if (commodityTransactionId.isBlank()) {
            throw new IllegalArgumentException("Commodity transaction ID cannot be blank");
        }

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
     * Convert to base Murabaha calculation (without commodity tracking).
     *
     * @return MurabahaCalculation with same financial terms
     */
    public MurabahaCalculation toMurabahaCalculation() {
        return new MurabahaCalculation(
                costPrice,
                profitAmount,
                salePrice,
                monthlyInstallment,
                schedule
        );
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
        return String.format("TawarruqCalculation[Commodity: %s, Cost: %s, Profit: %s, Sale: %s, Monthly: %s, Installments: %d]",
                commodityTransactionId,
                costPrice.toFormattedString(),
                profitAmount.toFormattedString(),
                salePrice.toFormattedString(),
                monthlyInstallment.toFormattedString(),
                getTotalInstallments()
        );
    }
}
