package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EmdhaMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "EMDHA";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "EMDHA_SIGN", "EMDHA_SIGN_DOCUMENT" -> signResponse();
            case "EMDHA_GENERATE_CONTRACT" -> signResponse();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult signResponse() {
        var body = """
                {
                    "success": true,
                    "message": "Documents signed and saved successfully",
                    "data": {
                        "signatureId": "%s",
                        "status": "SIGNED",
                        "documents": [
                            {
                                "documentId": "%s",
                                "fileName": "contract.pdf",
                                "signed": true,
                                "signedAt": "%s"
                            },
                            {
                                "documentId": "%s",
                                "fileName": "terms.pdf",
                                "signed": true,
                                "signedAt": "%s"
                            }
                        ],
                        "signerInfo": {
                            "kycId": "1108149475",
                            "mobileNo": "966590933288",
                            "englishName": "FAISAL",
                            "arabicName": "\u0641\u064a\u0635\u0644"
                        }
                    }
                }
                """.formatted(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                java.time.Instant.now().toString(),
                UUID.randomUUID().toString(),
                java.time.Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "success": true,
                    "message": "Documents signed and saved successfully",
                    "data": null
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }
}
