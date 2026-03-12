package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MasdarMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "MASDAR";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "MASDAR_AUTH" -> authResponse();
            case "MASDAR_INDIVIDUAL_PLUS" -> individualPlusResponse();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult authResponse() {
        var body = """
                {
                    "access_token": "mock-masdar-token-%s",
                    "token_type": "Bearer",
                    "expires_in": 3600
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult individualPlusResponse() {
        var body = """
                {
                    "referenceNumber": "REF-%s",
                    "individual": {
                        "nationalId": "1234567890",
                        "fullNameAr": "\u0645\u062d\u0645\u062f \u0627\u062e\u062a\u0628\u0627\u0631",
                        "fullNameEn": "Mohammed Test",
                        "dateOfBirth": "1990-01-15",
                        "gender": "MALE",
                        "nationality": "SA",
                        "maritalStatus": "MARRIED",
                        "numberOfDependents": 3
                    },
                    "employment": {
                        "employerName": "Test Company Ltd",
                        "sector": "PRIVATE",
                        "basicSalary": 12000.00,
                        "totalSalary": 15000.00,
                        "joinDate": "2020-06-01",
                        "status": "ACTIVE"
                    },
                    "address": {
                        "city": "Riyadh",
                        "district": "Al Olaya",
                        "postalCode": "12211",
                        "buildingNumber": "1234"
                    }
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "status": "Success",
                    "message": "MASDAR request processed"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }
}
