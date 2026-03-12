package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class SafeWatchMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "SAFEWATCH";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "SAFEWATCH_SCAN_SESSION" -> scanSession();
            case "SAFEWATCH_SCAN_DETAILS" -> scanDetails();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult scanSession() {
        var body = """
                {
                    "sessionId": "SCN-%s",
                    "status": "CREATED",
                    "scanType": "FULL",
                    "entityName": "Mohammed Al-Test",
                    "entityType": "INDIVIDUAL",
                    "listsScanned": ["SANCTIONS", "PEP", "ADVERSE_MEDIA"],
                    "createdAt": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult scanDetails() {
        var body = """
                {
                    "sessionId": "SCN-%s",
                    "status": "COMPLETED",
                    "hitCount": 0,
                    "riskLevel": "LOW",
                    "listsScanned": ["SANCTIONS", "PEP", "ADVERSE_MEDIA"],
                    "results": {
                        "sanctions": {"hits": 0, "status": "CLEAR"},
                        "pep": {"hits": 0, "status": "CLEAR"},
                        "adverseMedia": {"hits": 0, "status": "CLEAR"}
                    },
                    "completedAt": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "status": "Success",
                    "message": "SAFEWATCH request processed"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }
}
