package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class NafithMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "NAFITH";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "NAFITH_AUTH" -> authResponse();
            case "NAFITH_CREATE_SANAD" -> createSanad();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult authResponse() {
        var body = """
                {
                    "access_token": "mock-nafith-token-%s",
                    "token_type": "Bearer",
                    "expires_in": 3600
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult createSanad() {
        var body = """
                {
                    "sanadGroupId": "SND-%s",
                    "status": "CREATED",
                    "sanadCount": 12,
                    "totalAmount": 60000.00,
                    "monthlyAmount": 5000.00,
                    "creditorName": "KSA Financing Company",
                    "debtorNationalId": "1234567890",
                    "createdAt": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "status": "Success",
                    "message": "NAFITH request processed"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }
}
