package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class HyperpayMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "HYPERPAY";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "HYPERPAY_CHECKOUT" -> checkoutResponse();
            case "HYPERPAY_STATUS" -> statusResponse();
            case "HYPERPAY_REFUND" -> refundResponse();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult checkoutResponse() {
        var body = """
                {
                    "checkoutId": "CHK-%s",
                    "status": "CREATED",
                    "amount": 5000.00,
                    "currency": "SAR",
                    "paymentBrand": "MADA",
                    "redirectUrl": "https://mock.hyperpay.com/checkout/%s",
                    "expiresAt": "%s"
                }
                """.formatted(
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8),
                Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult statusResponse() {
        var body = """
                {
                    "checkoutId": "CHK-%s",
                    "status": "COMPLETED",
                    "amount": 5000.00,
                    "currency": "SAR",
                    "paymentBrand": "MADA",
                    "transactionId": "TXN-%s",
                    "completedAt": "%s"
                }
                """.formatted(
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8),
                Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult refundResponse() {
        var body = """
                {
                    "refundId": "RFD-%s",
                    "status": "REFUNDED",
                    "originalTransactionId": "TXN-%s",
                    "amount": 5000.00,
                    "currency": "SAR",
                    "refundedAt": "%s"
                }
                """.formatted(
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8),
                Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "status": "Success",
                    "message": "HYPERPAY request processed"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }
}
