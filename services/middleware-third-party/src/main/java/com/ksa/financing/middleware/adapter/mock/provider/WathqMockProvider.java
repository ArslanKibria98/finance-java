package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class WathqMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "WATHQ";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "WATHQ_VERIFY_CR" -> verifyCr();
            case "WATHQ_GET_COMPANY_INFO" -> getCompanyInfo();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult verifyCr() {
        var body = """
                {
                    "referenceNumber": "WTQ-%s",
                    "crNumber": "1010123456",
                    "companyNameAr": "\u0634\u0631\u0643\u0629 \u0627\u062e\u062a\u0628\u0627\u0631 \u0627\u0644\u0645\u062d\u062f\u0648\u062f\u0629",
                    "companyNameEn": "Test Company LLC",
                    "status": "ACTIVE",
                    "issueDate": "2020-01-15",
                    "expiryDate": "2027-01-14",
                    "businessType": "LIMITED_LIABILITY",
                    "city": "Riyadh",
                    "capital": 1000000.00,
                    "verifiedAt": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult getCompanyInfo() {
        var body = """
                {
                    "referenceNumber": "WTQ-%s",
                    "crNumber": "1010123456",
                    "companyNameAr": "\u0634\u0631\u0643\u0629 \u0627\u062e\u062a\u0628\u0627\u0631 \u0627\u0644\u0645\u062d\u062f\u0648\u062f\u0629",
                    "companyNameEn": "Test Company LLC",
                    "status": "ACTIVE",
                    "issueDate": "2020-01-15",
                    "expiryDate": "2027-01-14",
                    "businessType": "LIMITED_LIABILITY",
                    "city": "Riyadh",
                    "capital": 1000000.00,
                    "activities": [
                        {"code": "4719", "description": "Retail Trade"},
                        {"code": "6201", "description": "Computer Programming"}
                    ],
                    "shareholders": [
                        {"name": "Mohammed Al-Test", "share": 60.0},
                        {"name": "Ahmed Al-Partner", "share": 40.0}
                    ],
                    "retrievedAt": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "status": "Success",
                    "message": "WATHQ request processed"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }
}
