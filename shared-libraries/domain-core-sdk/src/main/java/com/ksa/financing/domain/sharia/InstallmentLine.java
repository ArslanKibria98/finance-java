package com.ksa.financing.domain.sharia;

import com.ksa.financing.domain.valueobject.SarMoney;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a single installment line in an amortization schedule.
 * <p>
 * Each installment contains detailed breakdown of principal and profit components,
 * along with cumulative tracking for audit and reporting purposes.
 * </p>
 * <p>
 * <b>Sharia Compliance:</b> In Islamic financing, the "profit component" represents
 * the predetermined markup (in Murabaha) or rental amount (in Ijara), not conventional
 * interest. The profit is known and agreed upon at contract inception (AAOIFI FAS 2, 8, 9).
 * </p>
 *
 * @param installmentNumber  The sequential number of this installment (1-based)
 * @param dueDate           The date when this installment payment is due
 * @param openingPrincipal  The principal balance at the start of this period
 * @param principalComponent The principal amount being repaid in this installment
 * @param profitComponent   The profit amount due in this installment
 * @param totalInstallment  The total payment due (principal + profit)
 * @param closingPrincipal  The remaining principal balance after this installment
 * @param cumulativePrincipal The total principal paid up to and including this installment
 * @param cumulativeProfit  The total profit paid up to and including this installment
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record InstallmentLine(
        int installmentNumber,
        LocalDate dueDate,
        SarMoney openingPrincipal,
        SarMoney principalComponent,
        SarMoney profitComponent,
        SarMoney totalInstallment,
        SarMoney closingPrincipal,
        SarMoney cumulativePrincipal,
        SarMoney cumulativeProfit
) {
    /**
     * Canonical constructor with validation.
     */
    public InstallmentLine {
        if (installmentNumber < 1) {
            throw new IllegalArgumentException("Installment number must be at least 1");
        }
        Objects.requireNonNull(dueDate, "Due date cannot be null");
        Objects.requireNonNull(openingPrincipal, "Opening principal cannot be null");
        Objects.requireNonNull(principalComponent, "Principal component cannot be null");
        Objects.requireNonNull(profitComponent, "Profit component cannot be null");
        Objects.requireNonNull(totalInstallment, "Total installment cannot be null");
        Objects.requireNonNull(closingPrincipal, "Closing principal cannot be null");
        Objects.requireNonNull(cumulativePrincipal, "Cumulative principal cannot be null");
        Objects.requireNonNull(cumulativeProfit, "Cumulative profit cannot be null");

        // Validate that total equals principal + profit
        SarMoney calculatedTotal = principalComponent.add(profitComponent);
        if (!calculatedTotal.equals(totalInstallment)) {
            throw new IllegalArgumentException(
                    String.format("Total installment must equal principal + profit. " +
                            "Expected: %s, Got: %s", calculatedTotal, totalInstallment)
            );
        }

        // Validate that closing balance equals opening balance minus principal payment
        SarMoney calculatedClosing = openingPrincipal.subtract(principalComponent);
        if (!calculatedClosing.equals(closingPrincipal)) {
            throw new IllegalArgumentException(
                    String.format("Closing principal must equal opening principal minus principal component. " +
                            "Expected: %s, Got: %s", calculatedClosing, closingPrincipal)
            );
        }

        // Validate non-negative amounts
        if (openingPrincipal.isNegative()) {
            throw new IllegalArgumentException("Opening principal cannot be negative");
        }
        if (principalComponent.isNegative()) {
            throw new IllegalArgumentException("Principal component cannot be negative");
        }
        if (profitComponent.isNegative()) {
            throw new IllegalArgumentException("Profit component cannot be negative");
        }
        if (closingPrincipal.isNegative()) {
            throw new IllegalArgumentException("Closing principal cannot be negative");
        }
    }

    /**
     * Check if this is the final installment (closing principal is zero).
     *
     * @return true if this is the final installment
     */
    public boolean isFinalInstallment() {
        return closingPrincipal.isZero();
    }

    /**
     * Get the profit-to-principal ratio for this installment.
     *
     * @return the ratio of profit to principal
     */
    public double getProfitToPrincipalRatio() {
        if (principalComponent.isZero()) {
            return 0.0;
        }
        return profitComponent.getValue()
                .divide(principalComponent.getValue(), 4, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Override
    public String toString() {
        return String.format("Installment #%d [Due: %s, Total: %s, Principal: %s, Profit: %s, Balance: %s]",
                installmentNumber,
                dueDate,
                totalInstallment.toFormattedString(),
                principalComponent.toFormattedString(),
                profitComponent.toFormattedString(),
                closingPrincipal.toFormattedString()
        );
    }
}
