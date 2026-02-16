package com.ksa.financing.domain.sharia;

import com.ksa.financing.domain.valueobject.SarMoney;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Result record containing complete Ijara (lease) financing calculations.
 * <p>
 * <b>Ijara Overview:</b> Ijara is an Islamic lease contract where the financier (lessor)
 * purchases an asset and leases it to the customer (lessee) for a specified rental amount.
 * The lessor retains ownership and bears ownership risks during the lease period.
 * </p>
 * <p>
 * <b>Sharia Compliance (AAOIFI FAS 8, 9):</b>
 * <ul>
 *   <li>Lessor must own the asset and bear ownership risks</li>
 *   <li>Asset must be identifiable and lawful (halal)</li>
 *   <li>Rental amount must be known and agreed upon</li>
 *   <li>Rental cannot be based on uncertain factors</li>
 *   <li>Lessor responsible for major maintenance</li>
 *   <li>May include promise to transfer ownership at end (Ijara Muntahia Bittamleek)</li>
 *   <li>Residual value represents either: buyout price or expected market value at lease end</li>
 * </ul>
 * </p>
 * <p>
 * <b>Formula:</b>
 * <pre>
 * Total Rental = (Asset Value - Residual Value) × (1 + Rental Yield)
 * Monthly Rental = Total Rental ÷ Tenure (months)
 * </pre>
 * </p>
 *
 * @param assetValue              The value of the leased asset
 * @param totalRental             The total rental amount over the lease period
 * @param monthlyRental           The fixed monthly rental payment
 * @param residualValue           The residual/buyout value at end of lease (can be zero)
 * @param ownershipTransferDate   Optional date when ownership transfers to lessee (Ijara Muntahia Bittamleek)
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record IjaraCalculation(
        SarMoney assetValue,
        SarMoney totalRental,
        SarMoney monthlyRental,
        SarMoney residualValue,
        LocalDate ownershipTransferDate
) {
    /**
     * Canonical constructor with validation.
     */
    public IjaraCalculation {
        Objects.requireNonNull(assetValue, "Asset value cannot be null");
        Objects.requireNonNull(totalRental, "Total rental cannot be null");
        Objects.requireNonNull(monthlyRental, "Monthly rental cannot be null");
        Objects.requireNonNull(residualValue, "Residual value cannot be null");
        // ownershipTransferDate is nullable (null for pure Ijara, non-null for Ijara Muntahia Bittamleek)

        // Validate all amounts are non-negative
        if (assetValue.isNegative()) {
            throw new IllegalArgumentException("Asset value cannot be negative");
        }
        if (totalRental.isNegative()) {
            throw new IllegalArgumentException("Total rental cannot be negative");
        }
        if (monthlyRental.isNegative()) {
            throw new IllegalArgumentException("Monthly rental cannot be negative");
        }
        if (residualValue.isNegative()) {
            throw new IllegalArgumentException("Residual value cannot be negative");
        }

        // Validate residual value doesn't exceed asset value
        if (residualValue.isGreaterThan(assetValue)) {
            throw new IllegalArgumentException(
                    String.format("Residual value cannot exceed asset value. Asset: %s, Residual: %s",
                            assetValue, residualValue)
            );
        }

        // Validate asset value is positive
        if (assetValue.isZero()) {
            throw new IllegalArgumentException("Asset value must be greater than zero");
        }
    }

    /**
     * Calculate the total amount customer will pay (rentals + residual).
     *
     * @return total payment amount
     */
    public SarMoney getTotalCustomerPayment() {
        return totalRental.add(residualValue);
    }

    /**
     * Calculate the lessor's profit (total rental - asset depreciation).
     *
     * @return lessor's profit
     */
    public SarMoney getLessorProfit() {
        SarMoney assetDepreciation = assetValue.subtract(residualValue);
        return totalRental.subtract(assetDepreciation);
    }

    /**
     * Calculate the effective yield rate.
     *
     * @return the yield rate as a decimal
     */
    public double getEffectiveYieldRate() {
        SarMoney depreciableAmount = assetValue.subtract(residualValue);
        if (depreciableAmount.isZero()) {
            return 0.0;
        }
        SarMoney profit = getLessorProfit();
        return profit.getValue()
                .divide(depreciableAmount.getValue(), 4, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Check if this is an Ijara Muntahia Bittamleek (lease ending with ownership transfer).
     *
     * @return true if ownership will transfer to lessee
     */
    public boolean isIjaraMuntahiaBittamleek() {
        return ownershipTransferDate != null;
    }

    /**
     * Get the type of Ijara contract.
     *
     * @return contract type description
     */
    public String getContractType() {
        return isIjaraMuntahiaBittamleek()
                ? "Ijara Muntahia Bittamleek (Lease-to-Own)"
                : "Ijara (Operating Lease)";
    }

    @Override
    public String toString() {
        return String.format("IjaraCalculation[%s, Asset: %s, Total Rental: %s, Monthly: %s, Residual: %s, Transfer: %s]",
                getContractType(),
                assetValue.toFormattedString(),
                totalRental.toFormattedString(),
                monthlyRental.toFormattedString(),
                residualValue.toFormattedString(),
                ownershipTransferDate != null ? ownershipTransferDate.toString() : "N/A"
        );
    }
}
