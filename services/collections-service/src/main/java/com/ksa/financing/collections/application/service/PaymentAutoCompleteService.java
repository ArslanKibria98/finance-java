package com.ksa.financing.collections.application.service;

import com.ksa.financing.collections.domain.port.in.ProcessPaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAutoCompleteService {

    private final ProcessPaymentUseCase paymentUseCase;

    @Async
    public void completePaymentAfterDelay(UUID tenantId, UUID paymentId, long delayMillis) {
        try {
            log.info("⏳ Scheduled auto-complete for payment {} in {}ms", paymentId, delayMillis);
            Thread.sleep(delayMillis);

            String providerTxn = "AUTO-" + System.currentTimeMillis();
            var command = new ProcessPaymentUseCase.CompletePaymentCommand(tenantId, paymentId, providerTxn);
            var result = paymentUseCase.completePayment(command);

            log.info("✅ Auto-completed payment: paymentId={} providerTxn={}", paymentId, providerTxn);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Auto-complete interrupted for paymentId={}: {}", paymentId, e.getMessage());
        } catch (Exception e) {
            log.error("❌ Auto-complete failed for paymentId={}: {}", paymentId, e.getMessage(), e);
        }
    }
}
