package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class AbsherMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "ABSHER";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "ABSHER_VERIFY_IDENTITY" -> verifyIdentityResponse();
            case "ABSHER_GET_ADDRESS" -> getAddressResponse();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult verifyIdentityResponse() {
        var body = """
                {
                    "referenceNumber": "ABS-%s",
                    "status": "VERIFIED",
                    "nationalId": "1234567890",
                    "verificationLevel": "HIGH",
                    "verifiedAt": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult getAddressResponse() {
        var body = """
                {
                    "referenceNumber": "ABS-%s",
                    "nationalId": "1234567890",
                    "address": {
                        "city": "Riyadh",
                        "district": "Al Olaya",
                        "street": "King Fahd Road",
                        "buildingNumber": "1234",
                        "postalCode": "12211",
                        "additionalNumber": "5678"
                    },
                    "retrievedAt": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "status": "Success",
                    "message": "ABSHER request processed"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }
}
