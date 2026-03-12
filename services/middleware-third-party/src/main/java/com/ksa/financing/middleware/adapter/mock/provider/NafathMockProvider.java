package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NafathMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "NAFATH";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "NAFATH_INITIATE" -> initiateResponse();
            case "NAFATH_CHECK_STATUS" -> checkStatusResponse();
            case "NAFATH_CALLBACK" -> callbackResponse();
            case "NAFATH_GET_JWK" -> getJwkResponse();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult initiateResponse() {
        var body = """
                {
                    "transId": "TRANS-%s",
                    "status": "Pending",
                    "requestId": "REQ-%s",
                    "random": "%d"
                }
                """.formatted(
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8),
                (int) (Math.random() * 90 + 10));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult checkStatusResponse() {
        var body = """
                {
                    "transId": "TRANS-%s",
                    "status": "COMPLETED",
                    "requestId": "REQ-%s"
                }
                """.formatted(
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult callbackResponse() {
        var body = """
                {
                    "status": "Received"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult getJwkResponse() {
        var body = """
                {
                    "keys": [
                        {
                            "kty": "RSA",
                            "kid": "mock-key-id",
                            "use": "sig",
                            "n": "mock-modulus",
                            "e": "AQAB"
                        }
                    ]
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "transId": "TRANS-%s",
                    "status": "Pending",
                    "requestId": "REQ-%s"
                }
                """.formatted(
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }
}
