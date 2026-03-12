package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class AnbMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "ANB";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "ANB_AUTH" -> authResponse();
            case "ANB_TRANSFER" -> transferResponse();
            case "ANB_ACCOUNT_INQUIRY" -> accountInquiryResponse();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult authResponse() {
        var body = """
                {
                    "access_token": "mock-anb-token-%s",
                    "token_type": "Bearer",
                    "expires_in": 3600
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult transferResponse() {
        var body = """
                {
                    "paymentId": "PAY-%s",
                    "status": "INITIATED",
                    "amount": 5000.00,
                    "currency": "SAR",
                    "beneficiaryIban": "SA0380000000608010167519",
                    "beneficiaryName": "Mohammed Al-Test",
                    "initiatedAt": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult accountInquiryResponse() {
        var body = """
                {
                    "accountNumber": "ACC-%s",
                    "iban": "SA0380000000608010167519",
                    "accountHolderName": "Mohammed Al-Test",
                    "currency": "SAR",
                    "balance": 25000.00,
                    "status": "ACTIVE",
                    "retrievedAt": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "status": "Success",
                    "message": "ANB request processed"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }
}
