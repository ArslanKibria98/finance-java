package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class YakeenMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "TAHAQUQ";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "TAHAQUQ_VERIFY_MOBILE" -> verifyMobile();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult verifyMobile() {
        var body = """
                {
                    "referenceNumber": "%s",
                    "id": "1088052343",
                    "mobile": "966501088642",
                    "isOwner": true
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "referenceNumber": "%s",
                    "id": "1088052343",
                    "mobile": "966501088642",
                    "isOwner": true
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }
}
