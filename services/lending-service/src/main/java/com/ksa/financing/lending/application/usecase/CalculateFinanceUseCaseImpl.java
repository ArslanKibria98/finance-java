package com.ksa.financing.lending.application.usecase;

import com.ksa.financing.lending.domain.port.in.CalculateFinanceUseCase;
import com.ksa.financing.lending.domain.port.out.ProductConfigPort;
import com.ksa.financing.lending.domain.service.FinanceCalculationResult;
import com.ksa.financing.lending.domain.service.FinanceCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalculateFinanceUseCaseImpl implements CalculateFinanceUseCase {

    private final ProductConfigPort productConfigPort;

    @Override
    public FinanceCalculationResult calculate(CalculateFinanceCommand command) {
        log.info("Calculating finance: amount={}, tenure={}, productId={}",
                command.amount(), command.tenureMonths(), command.productId());

        var config = productConfigPort.fetchProductConfig(
                command.tenantId(),
                command.productId(),
                command.amount(),
                command.tenureMonths()
        );

        // Validate product limits — collect errors
        var errors = validateProductLimits(command.amount(), command.tenureMonths(), config);
        if (!errors.isEmpty()) {
            // Return a result with errors — no calculation
            return FinanceCalculationResult.rejected(errors);
        }

        // Calculate processing fee from percentage if amount-based
        var processingFee = config.processingFeeAmount();
        if (processingFee == null || processingFee.compareTo(BigDecimal.ZERO) == 0) {
            if (config.processingFeePercent() != null) {
                processingFee = command.amount()
                        .multiply(config.processingFeePercent())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        }

        return FinanceCalculationService.calculate(
                command.amount(),
                config.profitRate(),
                command.tenureMonths(),
                processingFee,
                config.adminFeeAmount(),
                config.vatPercent(),
                config.isDisbursementInclusive()
        );
    }

    private List<String> validateProductLimits(BigDecimal amount, int tenureMonths,
                                                ProductConfigPort.ProductConfig config) {
        var errors = new ArrayList<String>();

        if (!config.fineractLinked()) {
            errors.add("Product '" + config.productName() + "' is not linked to any Fineract loan product. Please configure the product in Fineract first.");
            return errors;
        }

        if (config.minAmount() != null && amount.compareTo(config.minAmount()) < 0) {
            errors.add("Amount " + amount + " SAR is below minimum " + config.minAmount() + " SAR for this product");
        }
        if (config.maxAmount() != null && amount.compareTo(config.maxAmount()) > 0) {
            errors.add("Amount " + amount + " SAR exceeds maximum " + config.maxAmount() + " SAR for this product");
        }
        if (config.minTenureMonths() > 0 && tenureMonths < config.minTenureMonths()) {
            errors.add("Tenure " + tenureMonths + " months is below minimum " + config.minTenureMonths() + " months for this product");
        }
        if (config.maxTenureMonths() > 0 && tenureMonths > config.maxTenureMonths()) {
            errors.add("Tenure " + tenureMonths + " months exceeds maximum " + config.maxTenureMonths() + " months for this product");
        }

        return errors;
    }
}
