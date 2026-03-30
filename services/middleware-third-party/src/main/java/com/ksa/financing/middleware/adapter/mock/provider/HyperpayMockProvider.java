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
            case "HYPERPAY_REGISTRATION" -> registrationResponse();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult checkoutResponse() {
        var checkoutId = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 32);
        var body = """
                {
                    "result": {
                        "code": "000.200.100",
                        "description": "successfully created checkout"
                    },
                    "buildNumber": "mock-build-1",
                    "timestamp": "%s",
                    "ndc": "%s",
                    "id": "%s"
                }
                """.formatted(Instant.now().toString(), checkoutId, checkoutId);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult statusResponse() {
        var paymentId = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 32);
        var body = """
                {
                    "result": {
                        "code": "000.100.110",
                        "description": "Request successfully processed in 'Merchant in Integrator Test Mode'"
                    },
                    "buildNumber": "mock-build-1",
                    "timestamp": "%s",
                    "id": "%s",
                    "paymentType": "DB",
                    "paymentBrand": "MADA",
                    "amount": "5000.00",
                    "currency": "SAR",
                    "descriptor": "AWN Financing Payment",
                    "merchantTransactionId": "TXN-%s",
                    "resultDetails": {
                        "ConnectorTxID1": "MADA%s",
                        "AcquirerResponse": "00",
                        "AuthCode": "123456"
                    },
                    "card": {
                        "bin": "446404",
                        "last4Digits": "1846",
                        "holder": "FAISAL ALOTAIBI",
                        "expiryMonth": "05",
                        "expiryYear": "2028",
                        "country": "SA",
                        "type": "DEBIT"
                    },
                    "risk": {
                        "score": "0"
                    }
                }
                """.formatted(Instant.now().toString(), paymentId,
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 10).replaceAll("-", ""));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult refundResponse() {
        var refundId = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 32);
        var body = """
                {
                    "result": {
                        "code": "000.100.110",
                        "description": "Request successfully processed in 'Merchant in Integrator Test Mode'"
                    },
                    "buildNumber": "mock-build-1",
                    "timestamp": "%s",
                    "id": "%s",
                    "paymentType": "RF",
                    "amount": "5000.00",
                    "currency": "SAR",
                    "descriptor": "AWN Financing Refund"
                }
                """.formatted(Instant.now().toString(), refundId);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult registrationResponse() {
        var registrationId = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 32);
        var body = """
                {
                    "result": {
                        "code": "000.100.110",
                        "description": "Request successfully processed"
                    },
                    "buildNumber": "mock-build-1",
                    "timestamp": "%s",
                    "id": "%s",
                    "card": {
                        "bin": "446404",
                        "last4Digits": "1846",
                        "holder": "FAISAL ALOTAIBI",
                        "expiryMonth": "05",
                        "expiryYear": "2028"
                    }
                }
                """.formatted(Instant.now().toString(), registrationId);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "result": {
                        "code": "000.200.100",
                        "description": "Request processed successfully"
                    },
                    "timestamp": "%s"
                }
                """.formatted(Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }
}
