package com.demo.islamic.compliance.vat;

import com.demo.islamic.compliance.config.ComplianceConfig;
import com.demo.islamic.compliance.exception.VatCalculationException;
import com.demo.islamic.payment.model.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * VAT Calculator for Islamic Finance products
 * Implements KSA VAT rules: 15% on profit portion only
 * Principal amounts are NOT subject to VAT in Islamic finance
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VatCalculator {

    private static final BigDecimal KSA_VAT_RATE = new BigDecimal("0.15"); // 15%
    private static final int SCALE = 2; // SAR uses 2 decimal places

    private final ComplianceConfig complianceConfig;
    private final VatExemptionChecker exemptionChecker;

    /**
     * Calculate VAT on profit amount
     * In Islamic finance, VAT is applied only on the profit portion, not principal
     */
    public Money calculateVatOnProfit(Money profit) {
        if (profit == null || profit.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Money.of(BigDecimal.ZERO, profit != null ? profit.getCurrency() : "SAR");
        }

        BigDecimal vatAmount = profit.getAmount()
            .multiply(getApplicableVatRate())
            .setScale(SCALE, RoundingMode.HALF_UP);

        log.debug("VAT calculated on profit: profit={}, vatRate={}%, vat={}",
            profit.getAmount(), getApplicableVatRate().multiply(BigDecimal.valueOf(100)),
            vatAmount);

        return Money.of(vatAmount, profit.getCurrency());
    }

    /**
     * Calculate total amount including VAT
     * Formula: Total = Amount + (Amount × VAT Rate)
     */
    public Money calculateTotalWithVat(Money amount) {
        if (amount == null) {
            throw new VatCalculationException("Amount cannot be null");
        }

        Money vat = calculateVat(amount);
        BigDecimal total = amount.getAmount().add(vat.getAmount());

        return Money.of(total, amount.getCurrency());
    }

    /**
     * Calculate VAT amount from a base amount
     */
    public Money calculateVat(Money amount) {
        if (amount == null || amount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Money.of(BigDecimal.ZERO, amount != null ? amount.getCurrency() : "SAR");
        }

        BigDecimal vatAmount = amount.getAmount()
            .multiply(getApplicableVatRate())
            .setScale(SCALE, RoundingMode.HALF_UP);

        return Money.of(vatAmount, amount.getCurrency());
    }

    /**
     * Extract VAT from a VAT-inclusive amount
     * Formula: VAT = Total × (VAT Rate / (1 + VAT Rate))
     */
    public Money extractVatFromTotal(Money totalWithVat) {
        if (totalWithVat == null || totalWithVat.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Money.of(BigDecimal.ZERO, totalWithVat != null ? totalWithVat.getCurrency() : "SAR");
        }

        BigDecimal vatRate = getApplicableVatRate();
        BigDecimal vatFraction = vatRate.divide(
            BigDecimal.ONE.add(vatRate),
            6,
            RoundingMode.HALF_UP
        );

        BigDecimal vatAmount = totalWithVat.getAmount()
            .multiply(vatFraction)
            .setScale(SCALE, RoundingMode.HALF_UP);

        return Money.of(vatAmount, totalWithVat.getCurrency());
    }

    /**
     * Calculate VAT on Islamic finance installment
     * Only the profit component is subject to VAT
     */
    public VatBreakdown calculateInstallmentVat(
        Money principal, Money profit, String productType) {

        VatBreakdown breakdown = new VatBreakdown();
        breakdown.setPrincipalAmount(principal);
        breakdown.setProfitAmount(profit);
        breakdown.setProductType(productType);

        // Check if product is VAT exempt
        if (exemptionChecker.isExempt(productType)) {
            breakdown.setVatExempt(true);
            breakdown.setExemptionReason(exemptionChecker.getExemptionReason(productType));
            breakdown.setVatOnProfit(Money.of(BigDecimal.ZERO, profit.getCurrency()));
            breakdown.setTotalVat(Money.of(BigDecimal.ZERO, profit.getCurrency()));
            breakdown.setTotalAmount(principal.add(profit));

            log.info("Product is VAT exempt: productType={}, reason={}",
                productType, breakdown.getExemptionReason());

            return breakdown;
        }

        // Calculate VAT only on profit portion
        Money vatOnProfit = calculateVatOnProfit(profit);
        breakdown.setVatOnProfit(vatOnProfit);

        // Principal is NOT subject to VAT in Islamic finance
        breakdown.setVatOnPrincipal(Money.of(BigDecimal.ZERO, principal.getCurrency()));

        // Total VAT is only from profit
        breakdown.setTotalVat(vatOnProfit);

        // Total amount = Principal + Profit + VAT
        BigDecimal totalAmount = principal.getAmount()
            .add(profit.getAmount())
            .add(vatOnProfit.getAmount());
        breakdown.setTotalAmount(Money.of(totalAmount, principal.getCurrency()));

        breakdown.setVatRate(getApplicableVatRate());
        breakdown.setCalculationDate(LocalDate.now());

        log.debug("Installment VAT calculated: principal={}, profit={}, vat={}, total={}",
            principal.getAmount(), profit.getAmount(), vatOnProfit.getAmount(), totalAmount);

        return breakdown;
    }

    /**
     * Calculate VAT for admin or processing fees
     * These are always subject to VAT
     */
    public Money calculateFeeVat(Money feeAmount) {
        if (feeAmount == null || feeAmount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Money.of(BigDecimal.ZERO, feeAmount != null ? feeAmount.getCurrency() : "SAR");
        }

        // Fees are always VATable at standard rate
        BigDecimal vatAmount = feeAmount.getAmount()
            .multiply(KSA_VAT_RATE)
            .setScale(SCALE, RoundingMode.HALF_UP);

        log.debug("VAT calculated on fee: fee={}, vat={}",
            feeAmount.getAmount(), vatAmount);

        return Money.of(vatAmount, feeAmount.getCurrency());
    }

    /**
     * Validate VAT calculation for audit purposes
     */
    public boolean validateVatCalculation(Money baseAmount, Money vatAmount) {
        if (baseAmount == null || vatAmount == null) {
            return false;
        }

        Money calculatedVat = calculateVat(baseAmount);

        // Allow for rounding differences (0.01 SAR)
        BigDecimal difference = calculatedVat.getAmount()
            .subtract(vatAmount.getAmount())
            .abs();

        boolean isValid = difference.compareTo(new BigDecimal("0.01")) <= 0;

        if (!isValid) {
            log.warn("VAT validation failed: base={}, provided={}, calculated={}, diff={}",
                baseAmount.getAmount(), vatAmount.getAmount(),
                calculatedVat.getAmount(), difference);
        }

        return isValid;
    }

    /**
     * Get applicable VAT rate based on configuration
     */
    public BigDecimal getApplicableVatRate() {
        // Check if VAT is enabled
        if (!complianceConfig.isVatEnabled()) {
            return BigDecimal.ZERO;
        }

        // Check for custom rate in configuration
        BigDecimal customRate = complianceConfig.getCustomVatRate();
        if (customRate != null && customRate.compareTo(BigDecimal.ZERO) >= 0) {
            return customRate;
        }

        // Return standard KSA rate
        return KSA_VAT_RATE;
    }

    /**
     * Get VAT rate as percentage for display
     */
    public BigDecimal getVatRatePercentage() {
        return getApplicableVatRate().multiply(BigDecimal.valueOf(100));
    }

    /**
     * Format VAT amount for invoice display
     */
    public String formatVatAmount(Money vatAmount) {
        return String.format("SAR %.2f", vatAmount.getAmount());
    }

    /**
     * Get standard KSA VAT rate
     */
    public BigDecimal getVatRate() {
        return KSA_VAT_RATE;
    }

    /**
     * VAT breakdown for detailed reporting
     */
    public static class VatBreakdown {
        private Money principalAmount;
        private Money profitAmount;
        private Money vatOnPrincipal;
        private Money vatOnProfit;
        private Money totalVat;
        private Money totalAmount;
        private BigDecimal vatRate;
        private boolean isVatExempt;
        private String exemptionReason;
        private String productType;
        private LocalDate calculationDate;

        // Getters and setters
        public Money getPrincipalAmount() { return principalAmount; }
        public void setPrincipalAmount(Money principalAmount) { this.principalAmount = principalAmount; }

        public Money getProfitAmount() { return profitAmount; }
        public void setProfitAmount(Money profitAmount) { this.profitAmount = profitAmount; }

        public Money getVatOnPrincipal() { return vatOnPrincipal; }
        public void setVatOnPrincipal(Money vatOnPrincipal) { this.vatOnPrincipal = vatOnPrincipal; }

        public Money getVatOnProfit() { return vatOnProfit; }
        public void setVatOnProfit(Money vatOnProfit) { this.vatOnProfit = vatOnProfit; }

        public Money getTotalVat() { return totalVat; }
        public void setTotalVat(Money totalVat) { this.totalVat = totalVat; }

        public Money getTotalAmount() { return totalAmount; }
        public void setTotalAmount(Money totalAmount) { this.totalAmount = totalAmount; }

        public BigDecimal getVatRate() { return vatRate; }
        public void setVatRate(BigDecimal vatRate) { this.vatRate = vatRate; }

        public boolean isVatExempt() { return isVatExempt; }
        public void setVatExempt(boolean vatExempt) { isVatExempt = vatExempt; }

        public String getExemptionReason() { return exemptionReason; }
        public void setExemptionReason(String exemptionReason) { this.exemptionReason = exemptionReason; }

        public String getProductType() { return productType; }
        public void setProductType(String productType) { this.productType = productType; }

        public LocalDate getCalculationDate() { return calculationDate; }
        public void setCalculationDate(LocalDate calculationDate) { this.calculationDate = calculationDate; }
    }
}