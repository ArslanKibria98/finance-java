package com.ksa.financing.collections.adapter.rest.controller;

import com.ksa.financing.collections.adapter.rest.request.HyperPayCallbackRequest;
import com.ksa.financing.collections.adapter.rest.request.ProcessPaymentRequest;
import com.ksa.financing.collections.adapter.rest.response.HyperPayCheckoutResponse;
import com.ksa.financing.collections.adapter.rest.response.PaymentResponse;
import com.ksa.financing.collections.domain.model.PaymentAggregate;
import com.ksa.financing.collections.domain.model.PaymentMethod;
import com.ksa.financing.collections.domain.model.PaymentStatus;
import com.ksa.financing.collections.domain.port.in.ProcessPaymentUseCase;
import com.ksa.financing.collections.domain.port.in.ProcessPaymentUseCase.*;
import com.ksa.financing.collections.domain.port.out.HyperPayPort;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * HyperPay payment flow controller.
 *
 * Flow:
 * 1. POST /api/v1/payments/hyperpay/checkout  → creates payment record + calls middleware → returns checkoutId
 * 2. Customer renders HyperPay widget with checkoutId
 * 3. POST /api/v1/payments/hyperpay/callback  → middleware verifies status → completes/fails payment
 */
@RestController
@RequestMapping("/api/v1/payments/hyperpay")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "HyperPay", description = "HyperPay payment gateway integration via middleware")
public class HyperPayController {

    private final ProcessPaymentUseCase paymentUseCase;
    private final HyperPayPort hyperPayPort;

    @Value("${hyperpay.widget-url:https://eu-prod.oppwa.com/v1/paymentWidgets.js}")
    private String hyperPayWidgetUrl;

    @PostMapping("/checkout")
    @Operation(summary = "Initiate HyperPay checkout — creates checkout session via middleware")
    @SecuredEndpoint(obj = "payments", act = "create")
    public ResponseEntity<HyperPayCheckoutResponse> initiateCheckout(
            @Valid @RequestBody ProcessPaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID customerId = UUID.fromString(jwt.getSubject());
        String idempotencyKey = resolveIdempotencyKey(request.idempotencyKey());

        if (request.invoiceId() == null || request.invoiceId().isEmpty()) {
            throw new BusinessException("PAYMENT.HYPERPAY.INVOICE_REQUIRED",
                    "At least one invoiceId is required for HyperPay checkout");
        }
        if (request.invoiceId().size() > 1) {
            throw new BusinessException("PAYMENT.HYPERPAY.SINGLE_INVOICE_ONLY",
                    "HyperPay checkout supports a single invoiceId per request");
        }
        String invoiceId = request.invoiceId().get(0);

        // Step 1: Create payment record in PENDING state
        var command = new InitiatePaymentCommand(
                tenantId,
                request.loanId(),
                request.installmentId(),
                customerId,
                request.amount(),
                request.paymentMethod(),
                invoiceId,
                LocalDate.now(),
                null,
                idempotencyKey
        );
        var payment = paymentUseCase.initiatePayment(command);

        // Step 2: Call middleware → HyperPay to create checkout session
        String paymentBrand = toHyperPayBrand(request.paymentMethod());
        var checkoutResult = hyperPayPort.createCheckout(
                request.amount(), "SAR", paymentBrand, idempotencyKey);

        if (!checkoutResult.success()) {
            // Fail the payment record if checkout creation fails
            paymentUseCase.failPayment(new FailPaymentCommand(
                    tenantId, payment.getId().getValue(),
                    "HYPERPAY_CHECKOUT_FAILED", checkoutResult.resultDescription()));

            throw new BusinessException("PAYMENT.HYPERPAY.CHECKOUT_FAILED",
                    "HyperPay checkout creation failed: " + checkoutResult.resultDescription());
        }

        // Store checkoutId as source reference
        log.info("HyperPay checkout created: paymentId={} checkoutId={}",
                payment.getId().getValue(), checkoutResult.checkoutId());

        return ResponseEntity.status(HttpStatus.CREATED).body(new HyperPayCheckoutResponse(
                payment.getId().getValue(),
                checkoutResult.checkoutId(),
                request.amount(),
                "SAR",
                paymentBrand,
                "PENDING",
                hyperPayWidgetUrl + "?checkoutId=" + checkoutResult.checkoutId()
        ));
    }

    @PostMapping("/callback")
    @Operation(summary = "HyperPay payment callback — verifies status via middleware and settles payment")
    @SecuredEndpoint(obj = "payments", act = "update")
    public ResponseEntity<PaymentResponse> handleCallback(
            @Valid @RequestBody HyperPayCallbackRequest request,
            @RequestParam UUID paymentId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);

        // Step 1: Verify payment status with HyperPay via middleware
        var statusResult = hyperPayPort.getPaymentStatus(request.checkoutId());

        if (!statusResult.success()) {
            log.warn("HyperPay status check failed for checkoutId={}: {}",
                    request.checkoutId(), statusResult.resultDescription());
            var failed = paymentUseCase.failPayment(new FailPaymentCommand(
                    tenantId, paymentId,
                    "HYPERPAY_STATUS_CHECK_FAILED", statusResult.resultDescription()));
            return ResponseEntity.ok(toResponse(failed));
        }

        if (statusResult.paymentSuccessful()) {
            // Step 2a: Payment successful — complete and apply waterfall allocation
            log.info("HyperPay payment successful: checkoutId={} paymentId={}",
                    request.checkoutId(), statusResult.paymentId());
            var result = paymentUseCase.completePayment(new CompletePaymentCommand(
                    tenantId, paymentId, statusResult.paymentId()));
            return ResponseEntity.ok(toResponse(result.payment()));
        } else {
            // Step 2b: Payment failed
            log.warn("HyperPay payment unsuccessful: checkoutId={} code={}",
                    request.checkoutId(), statusResult.resultCode());
            var failed = paymentUseCase.failPayment(new FailPaymentCommand(
                    tenantId, paymentId,
                    statusResult.resultCode(), statusResult.resultDescription()));
            return ResponseEntity.ok(toResponse(failed));
        }
    }

    @PostMapping("/refund")
    @Operation(summary = "Refund a HyperPay payment via middleware")
    @SecuredEndpoint(obj = "payments", act = "update")
    public ResponseEntity<PaymentResponse> refundPayment(
            @RequestParam UUID paymentId,
            @RequestParam String reason,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);

        // Get existing payment to retrieve HyperPay transaction ID and amount
        var payment = paymentUseCase.getPayment(tenantId, paymentId);

        if (payment.getProviderTransactionId() == null) {
            throw new BusinessException("PAYMENT.HYPERPAY.NO_TRANSACTION_ID",
                    "No HyperPay transaction ID on payment: " + paymentId);
        }

        // Call HyperPay refund via middleware
        var refundResult = hyperPayPort.refundPayment(
                payment.getProviderTransactionId(), payment.getAmount(), "SAR");

        if (!refundResult.success()) {
            throw new BusinessException("PAYMENT.HYPERPAY.REFUND_FAILED",
                    "HyperPay refund failed: " + refundResult.resultDescription());
        }

        // Reverse the payment in our system
        var reversed = paymentUseCase.reversePayment(new ReversePaymentCommand(
                tenantId, paymentId, reason + " | HyperPay refundId: " + refundResult.refundId()));

        log.info("HyperPay payment refunded: paymentId={} refundId={}", paymentId, refundResult.refundId());
        return ResponseEntity.ok(toResponse(reversed));
    }

    // ==================== HELPERS ====================

    private String toHyperPayBrand(PaymentMethod method) {
        return switch (method) {
            case HYPERPAY_MADA -> "MADA";
            case HYPERPAY_VISA -> "VISA";
            case HYPERPAY_MASTERCARD -> "MASTER";
            case HYPERPAY_APPLE_PAY -> "APPLEPAY";
            default -> "MADA";
        };
    }

    private UUID extractTenantId(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException("COMMON.AUTH.ACCESS_DENIED", "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantId);
    }

    private String resolveIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return idempotencyKey;
    }

    private PaymentResponse toResponse(PaymentAggregate payment) {
        return new PaymentResponse(
                payment.getId().getValue(),
                payment.getLoanId(),
                payment.getInstallmentId(),
                payment.getInvoiceId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getIdempotencyKey(),
                payment.getSourceReference(),
                payment.getProviderTransactionId(),
                payment.getFailureCode(),
                payment.getFailureMessage(),
                payment.getCreatedAt(),
                payment.getStatus() == PaymentStatus.COMPLETED ? payment.getUpdatedAt() : null
        );
    }
}
