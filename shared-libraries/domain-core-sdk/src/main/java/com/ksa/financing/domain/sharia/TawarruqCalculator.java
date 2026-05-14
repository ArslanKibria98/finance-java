package com.ksa.financing.domain.sharia;

import com.ksa.financing.domain.valueobject.ProfitRate;
import com.ksa.financing.domain.valueobject.SarMoney;
import com.ksa.financing.domain.valueobject.Tenure;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Stateless calculator for Tawarruq (commodity Murabaha) financing.
 * <p>
 * <b>Tawarruq Definition:</b> A three-party financing structure where:
 * <ol>
 *   <li>Financier purchases commodity from Supplier A at cost price</li>
 *   <li>Financier sells commodity to Customer on deferred payment (cost + profit)</li>
 *   <li>Customer immediately sells commodity to Buyer B for cash (at/near cost)</li>
 *   <li>Customer receives liquidity; owes financier the deferred sale price</li>
 * </ol>
 * </p>
 * <p>
 * <b>Sharia Compliance Requirements (AAOIFI Sharia Standard 30):</b>
 * <ul>
 *   <li>Commodity must be real, tangible, and lawful (halal)</li>
 *   <li>Financier must actually purchase and own commodity before selling to customer</li>
 *   <li>Customer must take ownership before selling to third party</li>
 *   <li>All three sales must be genuine, arm's-length transactions</li>
 *   <li>Commodity cannot be gold, silver, or currencies (to avoid Riba al-Fadl)</li>
 *   <li>Common commodities: base metals (copper, aluminum), crude oil, agricultural products</li>
 *   <li>Customer cannot be obligated to sell to a specific party (voluntary sale)</li>
 *   <li>Each transaction must have real delivery (actual or constructive)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Scholarly Debate:</b> "Organized Tawarruq" (where financial institution arranges all
 * three sales) is controversial. Some scholars (OIC Fiqh Academy) discourage it if the
 * commodity transaction is purely a legal formality for liquidity. Others permit it if
 * all Sharia conditions are met. Implementation should follow local Sharia board guidance.
 * </p>
 * <p>
 * <b>Calculation:</b> Identical to Murabaha (Cost + Profit = Sale Price), but includes
 * commodity transaction tracking for audit and compliance.
 * </p>
 * <p>
 * <b>Example:</b>
 * <pre>
 * Cost Price (Commodity): SAR 100,000
 * Profit Rate: 5% (0.05)
 * Tenure: 12 months
 *
 * Profit Amount = 100,000 × 0.05 = SAR 5,000
 * Sale Price = 100,000 + 5,000 = SAR 105,000
 * Monthly Installment = 105,000 ÷ 12 = SAR 8,750
 *
 * Commodity Transaction:
 * 1. Financier buys commodity from Supplier A: SAR 100,000
 * 2. Financier sells to Customer on credit: SAR 105,000
 * 3. Customer sells to Buyer B for cash: ~SAR 100,000 (market price)
 * </pre>
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public final class TawarruqCalculator {

    // Private constructor to prevent instantiation
    private TawarruqCalculator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Calculate complete Tawarruq financing with commodity transaction tracking.
     * <p>
     * This is the standard calculation that includes commodity transaction ID
     * for compliance and audit purposes.
     * </p>
     *
     * @param costPrice              The cost price of the commodity
     * @param profitRate             The annual profit rate (markup percentage)
     * @param tenure                 The financing period in months
     * @param startDate              The contract start date
     * @param commodityTransactionId The unique ID tracking the commodity purchase/sale chain
     * @return Complete Tawarruq calculation result
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public static TawarruqCalculation calculate(
            SarMoney costPrice,
            ProfitRate profitRate,
            Tenure tenure,
            LocalDate startDate,
            String commodityTransactionId,
            SarMoney feeAmount
    ) {
        Objects.requireNonNull(costPrice, "Cost price cannot be null");
        Objects.requireNonNull(profitRate, "Profit rate cannot be null");
        Objects.requireNonNull(tenure, "Tenure cannot be null");
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(commodityTransactionId, "Commodity transaction ID cannot be null");
        Objects.requireNonNull(feeAmount, "Fee amount cannot be null");

        if (commodityTransactionId.isBlank()) {
            throw new IllegalArgumentException("Commodity transaction ID cannot be blank");
        }
        if (costPrice.isZero() || costPrice.isNegative()) {
            throw new IllegalArgumentException("Cost price must be positive");
        }

        // Calculate profit amount
        SarMoney profitAmount = profitRate.multiply(costPrice);

        // Calculate sale price
        SarMoney salePrice = costPrice.add(profitAmount);

        // Calculate monthly installment (Principal + Profit + Fee)
        SarMoney monthlyInstallment = salePrice.add(feeAmount).divide(tenure.months());

        // Generate amortization schedule (same as Murabaha - flat distribution)
        List<InstallmentLine> schedule = AmortizationScheduleGenerator.generateFlatSchedule(
                costPrice,
                profitRate,
                tenure,
                startDate,
                feeAmount
        );

        return new TawarruqCalculation(
                costPrice,
                profitAmount,
                salePrice,
                monthlyInstallment,
                schedule,
                commodityTransactionId
        );
    }

    /**
     * Calculate Tawarruq with auto-generated commodity transaction ID.
     * <p>
     * Generates a UUID for the commodity transaction if not provided.
     * </p>
     *
     * @param costPrice  The cost price of the commodity
     * @param profitRate The profit rate
     * @param tenure     The tenure
     * @param startDate  The start date
     * @return Complete Tawarruq calculation
     */
    public static TawarruqCalculation calculate(
            SarMoney costPrice,
            ProfitRate profitRate,
            Tenure tenure,
            LocalDate startDate
    ) {
        String commodityTransactionId = "TAWQ-" + UUID.randomUUID().toString();
        return calculate(costPrice, profitRate, tenure, startDate, commodityTransactionId, SarMoney.zero());
    }

    /**
     * Calculate Tawarruq with declining balance profit distribution.
     * <p>
     * Less common but supported for jurisdictions that permit it.
     * </p>
     *
     * @param costPrice              The cost price
     * @param profitRate             The profit rate
     * @param tenure                 The tenure
     * @param startDate              The start date
     * @param commodityTransactionId The commodity transaction ID
     * @return Tawarruq calculation with declining balance
     */
    public static TawarruqCalculation calculateDecliningBalance(
            SarMoney costPrice,
            ProfitRate profitRate,
            Tenure tenure,
            LocalDate startDate,
            String commodityTransactionId,
            SarMoney feeAmount
    ) {
        Objects.requireNonNull(costPrice, "Cost price cannot be null");
        Objects.requireNonNull(profitRate, "Profit rate cannot be null");
        Objects.requireNonNull(tenure, "Tenure cannot be null");
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(commodityTransactionId, "Commodity transaction ID cannot be null");
        Objects.requireNonNull(feeAmount, "Fee amount cannot be null");

        if (commodityTransactionId.isBlank()) {
            throw new IllegalArgumentException("Commodity transaction ID cannot be blank");
        }
        if (costPrice.isZero() || costPrice.isNegative()) {
            throw new IllegalArgumentException("Cost price must be positive");
        }

        // Generate declining balance schedule
        List<InstallmentLine> schedule = AmortizationScheduleGenerator.generateDecliningBalanceSchedule(
                costPrice,
                profitRate,
                tenure,
                startDate,
                feeAmount
        );

        // Calculate totals from schedule
        SarMoney totalProfit = schedule.stream()
                .map(InstallmentLine::profitComponent)
                .reduce(SarMoney.zero(), SarMoney::add);

        SarMoney salePrice = costPrice.add(totalProfit);

        // Monthly installment is from schedule
        SarMoney monthlyInstallment = schedule.get(0).totalInstallment();

        return new TawarruqCalculation(
                costPrice,
                totalProfit,
                salePrice,
                monthlyInstallment,
                schedule,
                commodityTransactionId
        );
    }

    /**
     * Convert from Murabaha calculation to Tawarruq by adding commodity tracking.
     * <p>
     * Useful when a Murabaha structure needs commodity transaction tracking added.
     * </p>
     *
     * @param murabahaCalculation    The existing Murabaha calculation
     * @param commodityTransactionId The commodity transaction ID
     * @return Tawarruq calculation
     */
    public static TawarruqCalculation fromMurabaha(
            MurabahaCalculation murabahaCalculation,
            String commodityTransactionId
    ) {
        Objects.requireNonNull(murabahaCalculation, "Murabaha calculation cannot be null");
        Objects.requireNonNull(commodityTransactionId, "Commodity transaction ID cannot be null");

        if (commodityTransactionId.isBlank()) {
            throw new IllegalArgumentException("Commodity transaction ID cannot be blank");
        }

        return new TawarruqCalculation(
                murabahaCalculation.costPrice(),
                murabahaCalculation.profitAmount(),
                murabahaCalculation.salePrice(),
                murabahaCalculation.monthlyInstallment(),
                murabahaCalculation.schedule(),
                commodityTransactionId
        );
    }

    /**
     * Calculate only the profit amount (without full calculation).
     *
     * @param costPrice  The cost price
     * @param profitRate The profit rate
     * @return The profit amount
     */
    public static SarMoney calculateProfitAmount(SarMoney costPrice, ProfitRate profitRate) {
        return MurabahaCalculator.calculateProfitAmount(costPrice, profitRate);
    }

    /**
     * Calculate only the sale price.
     *
     * @param costPrice  The cost price
     * @param profitRate The profit rate
     * @return The sale price
     */
    public static SarMoney calculateSalePrice(SarMoney costPrice, ProfitRate profitRate) {
        return MurabahaCalculator.calculateSalePrice(costPrice, profitRate);
    }

    /**
     * Calculate only the monthly installment.
     *
     * @param costPrice  The cost price
     * @param profitRate The profit rate
     * @param tenure     The tenure
     * @return The monthly installment
     */
    public static SarMoney calculateMonthlyInstallment(
            SarMoney costPrice,
            ProfitRate profitRate,
            Tenure tenure,
            SarMoney feeAmount
    ) {
        return MurabahaCalculator.calculateMonthlyInstallment(costPrice, profitRate, tenure, feeAmount);
    }

    /**
     * Validate if parameters meet Sharia compliance requirements for Tawarruq.
     * <p>
     * Note: This only validates parameters. Actual commodity transaction compliance
     * (real purchase, ownership transfer, etc.) must be validated separately by
     * business logic and Sharia board.
     * </p>
     *
     * @param costPrice              The cost price
     * @param profitRate             The profit rate
     * @param tenure                 The tenure
     * @param commodityTransactionId The commodity transaction ID
     * @return true if parameters are valid
     */
    public static boolean isValidForTawarruq(
            SarMoney costPrice,
            ProfitRate profitRate,
            Tenure tenure,
            String commodityTransactionId
    ) {
        try {
            Objects.requireNonNull(commodityTransactionId, "Commodity transaction ID cannot be null");

            return !commodityTransactionId.isBlank()
                    && MurabahaCalculator.isValidForMurabaha(costPrice, profitRate, tenure);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate a commodity transaction ID with custom prefix.
     *
     * @param prefix The prefix (e.g., "TAWQ", "COMM")
     * @return Generated transaction ID
     */
    public static String generateCommodityTransactionId(String prefix) {
        Objects.requireNonNull(prefix, "Prefix cannot be null");
        if (prefix.isBlank()) {
            prefix = "TAWQ";
        }
        return prefix + "-" + UUID.randomUUID().toString();
    }

    /**
     * Generate a standard commodity transaction ID.
     *
     * @return Generated transaction ID with "TAWQ" prefix
     */
    public static String generateCommodityTransactionId() {
        return generateCommodityTransactionId("TAWQ");
    }
}
