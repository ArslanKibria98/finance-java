package com.ksa.financing.collections.infrastructure.messaging;

import com.ksa.financing.collections.domain.port.in.ManageRepaymentScheduleUseCase;
import com.ksa.financing.domain.sharia.AmortizationScheduleGenerator;
import com.ksa.financing.domain.valueobject.ProfitRate;
import com.ksa.financing.domain.valueobject.SarMoney;
import com.ksa.financing.domain.valueobject.Tenure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Listens to loan events from lending-service and manages repayment schedules in collections-service.
 * When a loan is created, automatically generates and stores the repayment schedule with installments.
 */
@Slf4j
//@Component // Disabled - Kafka deserialization issues
@RequiredArgsConstructor
public class LoanEventListener {

    private final ManageRepaymentScheduleUseCase scheduleUseCase;

    /**
     * Listens to financing.loan.loan-created topic.
     * Event payload contains: loanId, tenantId, applicationId, customerId, principalAmount, profitRate, tenureMonths, shariaStructure
     */
    @KafkaListener(
        topics = "financing.loan.loan-created",
        groupId = "${spring.application.name}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onLoanCreated(
            @Payload java.util.Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        try {
            log.info("Received LoanCreated event from topic={}", topic);

            // Extract event fields
            String loanIdStr = (String) event.get("loanId");
            String tenantIdStr = (String) event.get("tenantId");
            Object principalObj = event.get("principalAmount");
            Object profitRateObj = event.get("profitRate");
            Object tenureObj = event.get("tenureMonths");

            if (loanIdStr == null || tenantIdStr == null || principalObj == null || profitRateObj == null || tenureObj == null) {
                log.warn("LoanCreated event missing required fields: loanId={}, tenantId={}, principal={}, rate={}, tenure={}",
                        loanIdStr, tenantIdStr, principalObj, profitRateObj, tenureObj);
                return;
            }

            UUID loanId = UUID.fromString(loanIdStr);
            UUID tenantId = UUID.fromString(tenantIdStr);

            // Parse values
            BigDecimal principalAmount = new BigDecimal(principalObj.toString());
            BigDecimal profitRateValue = new BigDecimal(profitRateObj.toString());
            Integer tenureMonths = ((Number) tenureObj).intValue();

            log.info("Creating repayment schedule for loan: {} principal: {} profitRate: {} tenureMonths: {}",
                    loanId, principalAmount, profitRateValue, tenureMonths);

            // Convert to domain value objects
            SarMoney principal = SarMoney.of(principalAmount);
            ProfitRate rate = ProfitRate.ofDecimal(profitRateValue);
            Tenure tenure = Tenure.ofMonths(tenureMonths);
            LocalDate startDate = LocalDate.now();

            // Generate amortization schedule using domain-core-sdk (flat rate Murabaha)
            var amortization = AmortizationScheduleGenerator.generateFlatSchedule(
                    principal, rate, tenure, startDate);

            if (amortization == null || amortization.isEmpty()) {
                log.error("Failed to generate amortization schedule for loan: {}", loanId);
                return;
            }

            // Calculate first due date (first installment is due 1 month from now)
            LocalDate firstDueDate = LocalDate.now().plusMonths(1);
            LocalDate lastDueDate = firstDueDate.plusMonths(tenureMonths - 1);

            // Build installment list from amortization schedule
            List<ManageRepaymentScheduleUseCase.CreateScheduleCommand.InstallmentEntry> installments = new ArrayList<>();
            BigDecimal totalProfit = BigDecimal.ZERO;

            for (var entry : amortization) {
                int installmentNumber = entry.installmentNumber();
                LocalDate dueDate = entry.dueDate();

                installments.add(new ManageRepaymentScheduleUseCase.CreateScheduleCommand.InstallmentEntry(
                        installmentNumber,
                        dueDate,
                        entry.principalComponent().getValue(),  // principal for this month
                        entry.profitComponent().getValue(),     // profit (interest) for this month
                        BigDecimal.ZERO                         // no additional fees
                ));

                totalProfit = totalProfit.add(entry.profitComponent().getValue());
            }

            // Generate deterministic schedule number
            String scheduleNumber = "SCH-" + loanId.toString().substring(0, 8).toUpperCase() + "-001";

            // Create schedule using use case
            // productId not yet plumbed in loan-created event; falls back to tenant-default rules
            UUID productId = null;
            Object productIdObj = event.get("productId");
            if (productIdObj != null) {
                try {
                    productId = UUID.fromString(productIdObj.toString());
                } catch (IllegalArgumentException ignored) {
                    log.debug("Ignoring malformed productId in loan event: {}", productIdObj);
                }
            }

            var command = new ManageRepaymentScheduleUseCase.CreateScheduleCommand(
                    tenantId,
                    loanId,
                    productId,
                    scheduleNumber,
                    principalAmount,
                    totalProfit,
                    firstDueDate,
                    lastDueDate,
                    installments,
                    UUID.randomUUID()  // system user
            );

            var schedule = scheduleUseCase.createSchedule(command);
            log.info("✅ Repayment schedule created successfully: scheduleId={} loanId={} totalInstallments={}",
                    schedule.getId().getValue(), loanId, installments.size());

        } catch (Exception e) {
            log.error("Error processing LoanCreated event: {}", e.getMessage(), e);
            // Don't throw - allow Kafka to retry based on error handler configuration
        }
    }
}
