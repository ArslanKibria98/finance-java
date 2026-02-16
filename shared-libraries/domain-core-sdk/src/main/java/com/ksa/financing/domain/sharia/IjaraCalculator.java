package com.ksa.financing.domain.sharia;

import com.ksa.financing.domain.valueobject.SarMoney;
import com.ksa.financing.domain.valueobject.Tenure;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Stateless calculator for Ijara (Islamic lease) financing.
 * <p>
 * <b>Ijara Definition:</b> A lease contract where the lessor (financier) purchases an asset
 * and leases it to the lessee (customer) for a specified rental amount. The lessor retains
 * ownership and bears the risks and responsibilities of ownership.
 * </p>
 * <p>
 * <b>Sharia Compliance Requirements (AAOIFI FAS 8, 9):</b>
 * <ul>
 *   <li>Lessor must own the asset and bear ownership risks (damage, loss, maintenance)</li>
 *   <li>Asset must be identifiable, lawful (halal), and capable of being leased</li>
 *   <li>Rental amount must be known, fixed, and agreed upon at contract start</li>
 *   <li>Rental cannot be contingent on uncertain events</li>
 *   <li>Lessee responsible for routine maintenance; lessor for major repairs</li>
 *   <li>May include promise to transfer ownership at end (Ijara Muntahia Bittamleek)</li>
 *   <li>Asset cannot be consumable items (must remain intact during lease)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Types:</b>
 * <ul>
 *   <li><b>Ijara (Operating Lease):</b> Asset returns to lessor at end</li>
 *   <li><b>Ijara Muntahia Bittamleek (Lease-to-Own):</b> Asset ownership transfers to lessee</li>
 * </ul>
 * </p>
 * <p>
 * <b>Calculation Formula:</b>
 * <pre>
 * Depreciable Amount = Asset Value - Residual Value
 * Total Rental = Depreciable Amount × (1 + Rental Yield)
 * Monthly Rental = Total Rental ÷ Tenure (months)
 * </pre>
 * </p>
 * <p>
 * <b>Example:</b>
 * <pre>
 * Asset Value: SAR 100,000
 * Rental Yield: 10% (0.10)
 * Residual Value: SAR 20,000 (buyout price or expected value at end)
 * Tenure: 60 months
 *
 * Depreciable Amount = 100,000 - 20,000 = SAR 80,000
 * Total Rental = 80,000 × 1.10 = SAR 88,000
 * Monthly Rental = 88,000 ÷ 60 = SAR 1,466.67
 *
 * Total Customer Payment = 88,000 + 20,000 = SAR 108,000
 * </pre>
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public final class IjaraCalculator {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    // Private constructor to prevent instantiation
    private IjaraCalculator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Calculate Ijara lease financing with residual value.
     * <p>
     * Standard calculation for both operating lease (Ijara) and lease-to-own
     * (Ijara Muntahia Bittamleek).
     * </p>
     *
     * @param assetValue              The purchase value of the asset
     * @param rentalYield             The rental yield rate (e.g., 0.10 for 10%)
     * @param tenure                  The lease period
     * @param residualValue           The residual/buyout value at end (zero for full depreciation)
     * @param ownershipTransferDate   Date ownership transfers (null for operating lease)
     * @return Complete Ijara calculation result
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public static IjaraCalculation calculate(
            SarMoney assetValue,
            BigDecimal rentalYield,
            Tenure tenure,
            SarMoney residualValue,
            LocalDate ownershipTransferDate
    ) {
        Objects.requireNonNull(assetValue, "Asset value cannot be null");
        Objects.requireNonNull(rentalYield, "Rental yield cannot be null");
        Objects.requireNonNull(tenure, "Tenure cannot be null");
        Objects.requireNonNull(residualValue, "Residual value cannot be null");
        // ownershipTransferDate is nullable

        if (assetValue.isZero() || assetValue.isNegative()) {
            throw new IllegalArgumentException("Asset value must be positive");
        }
        if (residualValue.isNegative()) {
            throw new IllegalArgumentException("Residual value cannot be negative");
        }
        if (residualValue.isGreaterThan(assetValue)) {
            throw new IllegalArgumentException("Residual value cannot exceed asset value");
        }
        if (rentalYield.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Rental yield cannot be negative");
        }
        if (rentalYield.compareTo(new BigDecimal("1.0")) > 0) {
            throw new IllegalArgumentException("Rental yield cannot exceed 100%");
        }

        // Calculate depreciable amount (portion of asset to be recovered via rentals)
        SarMoney depreciableAmount = assetValue.subtract(residualValue);

        // Calculate total rental (depreciable amount + yield)
        BigDecimal yieldMultiplier = BigDecimal.ONE.add(rentalYield);
        SarMoney totalRental = depreciableAmount.multiply(yieldMultiplier);

        // Calculate monthly rental
        SarMoney monthlyRental = totalRental.divide(tenure.months());

        return new IjaraCalculation(
                assetValue,
                totalRental,
                monthlyRental,
                residualValue,
                ownershipTransferDate
        );
    }

    /**
     * Calculate Ijara Muntahia Bittamleek (lease-to-own) with ownership transfer.
     * <p>
     * This is a convenience method for lease-to-own structures where ownership
     * transfers at the end of the lease period.
     * </p>
     *
     * @param assetValue    The asset value
     * @param rentalYield   The rental yield rate
     * @param tenure        The lease period
     * @param residualValue The buyout/residual value
     * @param startDate     The lease start date
     * @return Ijara calculation with ownership transfer
     */
    public static IjaraCalculation calculateLeaseToOwn(
            SarMoney assetValue,
            BigDecimal rentalYield,
            Tenure tenure,
            SarMoney residualValue,
            LocalDate startDate
    ) {
        Objects.requireNonNull(startDate, "Start date cannot be null");
        LocalDate ownershipTransferDate = startDate.plusMonths(tenure.months());

        return calculate(assetValue, rentalYield, tenure, residualValue, ownershipTransferDate);
    }

    /**
     * Calculate operating lease (no ownership transfer).
     * <p>
     * Asset returns to lessor at end of lease. Typically used for equipment
     * or vehicles with significant residual value.
     * </p>
     *
     * @param assetValue    The asset value
     * @param rentalYield   The rental yield rate
     * @param tenure        The lease period
     * @param residualValue Expected market value at end of lease
     * @return Ijara calculation without ownership transfer
     */
    public static IjaraCalculation calculateOperatingLease(
            SarMoney assetValue,
            BigDecimal rentalYield,
            Tenure tenure,
            SarMoney residualValue
    ) {
        return calculate(assetValue, rentalYield, tenure, residualValue, null);
    }

    /**
     * Calculate total rental only (without full calculation).
     *
     * @param assetValue    The asset value
     * @param rentalYield   The rental yield rate
     * @param residualValue The residual value
     * @return The total rental amount
     */
    public static SarMoney calculateTotalRental(
            SarMoney assetValue,
            BigDecimal rentalYield,
            SarMoney residualValue
    ) {
        Objects.requireNonNull(assetValue, "Asset value cannot be null");
        Objects.requireNonNull(rentalYield, "Rental yield cannot be null");
        Objects.requireNonNull(residualValue, "Residual value cannot be null");

        SarMoney depreciableAmount = assetValue.subtract(residualValue);
        BigDecimal yieldMultiplier = BigDecimal.ONE.add(rentalYield);
        return depreciableAmount.multiply(yieldMultiplier);
    }

    /**
     * Calculate monthly rental only.
     *
     * @param assetValue    The asset value
     * @param rentalYield   The rental yield rate
     * @param tenure        The lease period
     * @param residualValue The residual value
     * @return The monthly rental amount
     */
    public static SarMoney calculateMonthlyRental(
            SarMoney assetValue,
            BigDecimal rentalYield,
            Tenure tenure,
            SarMoney residualValue
    ) {
        Objects.requireNonNull(tenure, "Tenure cannot be null");
        SarMoney totalRental = calculateTotalRental(assetValue, rentalYield, residualValue);
        return totalRental.divide(tenure.months());
    }

    /**
     * Calculate rental yield from known rental and asset values.
     * <p>
     * Reverse calculation: given rental amount, determine the implicit yield rate.
     * </p>
     *
     * @param totalRental   The total rental amount
     * @param assetValue    The asset value
     * @param residualValue The residual value
     * @return The rental yield rate as decimal
     */
    public static BigDecimal calculateImpliedYield(
            SarMoney totalRental,
            SarMoney assetValue,
            SarMoney residualValue
    ) {
        Objects.requireNonNull(totalRental, "Total rental cannot be null");
        Objects.requireNonNull(assetValue, "Asset value cannot be null");
        Objects.requireNonNull(residualValue, "Residual value cannot be null");

        SarMoney depreciableAmount = assetValue.subtract(residualValue);
        if (depreciableAmount.isZero()) {
            return BigDecimal.ZERO;
        }

        // Yield = (Total Rental / Depreciable Amount) - 1
        BigDecimal ratio = totalRental.getValue()
                .divide(depreciableAmount.getValue(), 6, ROUNDING_MODE);
        return ratio.subtract(BigDecimal.ONE).setScale(4, ROUNDING_MODE);
    }

    /**
     * Calculate residual value percentage of asset value.
     *
     * @param residualValue The residual value
     * @param assetValue    The asset value
     * @return Residual percentage as decimal (e.g., 0.20 for 20%)
     */
    public static BigDecimal calculateResidualPercentage(
            SarMoney residualValue,
            SarMoney assetValue
    ) {
        Objects.requireNonNull(residualValue, "Residual value cannot be null");
        Objects.requireNonNull(assetValue, "Asset value cannot be null");

        if (assetValue.isZero()) {
            return BigDecimal.ZERO;
        }

        return residualValue.getValue()
                .divide(assetValue.getValue(), 4, ROUNDING_MODE);
    }

    /**
     * Validate if parameters meet Sharia compliance requirements.
     *
     * @param assetValue    The asset value
     * @param rentalYield   The rental yield
     * @param tenure        The tenure
     * @param residualValue The residual value
     * @return true if parameters are valid
     */
    public static boolean isValidForIjara(
            SarMoney assetValue,
            BigDecimal rentalYield,
            Tenure tenure,
            SarMoney residualValue
    ) {
        try {
            Objects.requireNonNull(assetValue, "Asset value cannot be null");
            Objects.requireNonNull(rentalYield, "Rental yield cannot be null");
            Objects.requireNonNull(tenure, "Tenure cannot be null");
            Objects.requireNonNull(residualValue, "Residual value cannot be null");

            return assetValue.isPositive()
                    && rentalYield.compareTo(BigDecimal.ZERO) >= 0
                    && rentalYield.compareTo(new BigDecimal("1.0")) <= 0
                    && tenure.months() >= 1
                    && !residualValue.isNegative()
                    && !residualValue.isGreaterThan(assetValue);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calculate total customer payment (rentals + residual).
     *
     * @param totalRental   The total rental amount
     * @param residualValue The residual/buyout value
     * @return Total amount customer pays
     */
    public static SarMoney calculateTotalCustomerPayment(
            SarMoney totalRental,
            SarMoney residualValue
    ) {
        Objects.requireNonNull(totalRental, "Total rental cannot be null");
        Objects.requireNonNull(residualValue, "Residual value cannot be null");
        return totalRental.add(residualValue);
    }

    /**
     * Calculate lessor's profit (rental income minus asset depreciation).
     *
     * @param totalRental   The total rental received
     * @param assetValue    The asset's purchase value
     * @param residualValue The asset's ending value
     * @return Lessor's profit
     */
    public static SarMoney calculateLessorProfit(
            SarMoney totalRental,
            SarMoney assetValue,
            SarMoney residualValue
    ) {
        Objects.requireNonNull(totalRental, "Total rental cannot be null");
        Objects.requireNonNull(assetValue, "Asset value cannot be null");
        Objects.requireNonNull(residualValue, "Residual value cannot be null");

        SarMoney assetDepreciation = assetValue.subtract(residualValue);
        return totalRental.subtract(assetDepreciation);
    }
}
