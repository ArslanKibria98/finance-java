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
                    "requestNumber": "%s",
                    "message": "Information Retreived from GOSI",
                    "employmentStatusInfo": [
                        {
                            "fullName": "\u0641\u064a\u0635\u0644 \u0647\u0644\u064a\u0644 \u0639\u0628\u064a\u062f \u0627\u0644\u0639\u062a\u064a\u0628\u064a",
                            "basicWage": 7408.0,
                            "housingAllowance": 1851.0,
                            "otherAllowance": 0.0,
                            "employerName": "\u0634\u0631\u0643\u0629 \u0639\u0648\u0646 \u0627\u0644\u0631\u0627\u0626\u062f\u0629 \u0644\u0644\u062a\u0645\u0648\u064a\u0644 \u0627\u0644\u0627\u0633\u062a\u0647\u0644\u0627\u0643\u064a \u0627\u0644\u0645\u0635\u063a\u0631",
                            "workingMonths": "54",
                            "employmentStatus": "\u0646\u0634\u064a\u0637"
                        }
                    ]
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult govtResponse() {
        var body = """
                {
                    "requestNumber": "%s",
                    "message": "Information Retreived from GOSI",
                    "employmentStatusInfo": [
                        {
                            "fullName": "\u0641\u064a\u0635\u0644 \u0647\u0644\u064a\u0644 \u0639\u0628\u064a\u062f \u0627\u0644\u0639\u062a\u064a\u0628\u064a",
                            "basicWage": 12000.0,
                            "housingAllowance": 3000.0,
                            "otherAllowance": 1500.0,
                            "employerName": "Ministry of Finance",
                            "workingMonths": "36",
                            "employmentStatus": "\u0646\u0634\u064a\u0637"
                        }
                    ]
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "requestNumber": "%s",
                    "message": "Information Retreived from GOSI",
                    "employmentStatusInfo": []
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }
}
