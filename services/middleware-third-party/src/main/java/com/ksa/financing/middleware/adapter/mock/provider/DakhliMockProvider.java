package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DakhliMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "DAKHLI";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "DAKHLI_GOSI" -> gosiResponse();
            case "DAKHLI_GOVT" -> govtResponse();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult gosiResponse() {
        var body = """
                {
                    "requestNumber": "REQ-%s",
                    "message": "Information Retrieved",
                    "employmentStatusInfo": [
                        {
                            "fullName": "Test Employee",
                            "basicWage": 10000,
                            "housingAllowance": 2000,
                            "otherAllowance": 1000,
                            "employerName": "Test Company",
                            "workingMonths": 24,
                            "employmentStatus": "ACTIVE"
                        }
                    ]
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult govtResponse() {
        var body = """
                {
                    "requestNumber": "REQ-%s",
                    "message": "Government Employment Retrieved",
                    "employmentStatusInfo": [
                        {
                            "fullName": "Test Employee",
                            "basicWage": 12000,
                            "housingAllowance": 3000,
                            "otherAllowance": 1500,
                            "employerName": "Ministry of Finance",
                            "workingMonths": 36,
                            "employmentStatus": "ACTIVE",
                            "sector": "GOVERNMENT"
                        }
                    ]
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "status": "Success",
                    "message": "DAKHLI request processed"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }
}
