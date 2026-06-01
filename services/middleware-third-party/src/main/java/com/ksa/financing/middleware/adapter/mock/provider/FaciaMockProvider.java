package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Facia mock provider used when the middleware is hit by a TEST client.
 *
 * <p>Returns Facia-shaped envelopes:</p>
 * <pre>
 *   { status, message, result: { data: { ... } } }
 * </pre>
 *
 * <p>Mirrors the shape that the live Facia.ai service returns so the unwrapping
 * logic in {@code ExecuteApiService.unwrapProviderEnvelope} + downstream
 * {@code FaciaClient.unwrapFaciaResult} produces identical extracted payloads
 * regardless of whether the call landed on the mock or live provider.</p>
 */
@Component
public class FaciaMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "FACIA";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "FACIA_DOC_VERIFY" -> documentVerificationResponse();
            case "FACIA_FACE_MATCH" -> faceMatchResponse();
            case "FACIA_REQUEST_ACCESS_TOKEN" -> accessTokenResponse();
            default -> fallbackResponse(apiCode);
        };
    }

    private MockResponseResult documentVerificationResponse() {
        String referenceId = "mock-doc-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String body = """
                {
                  "status": 200,
                  "message": "success",
                  "result": {
                    "data": {
                      "event": "verification_accepted",
                      "reference_id": "%s",
                      "decline_reason": null,
                      "ocr_results": {
                        "name": {
                          "first_name": "Arslan",
                          "middle_name": null,
                          "last_name": "Kibria",
                          "full_name": "Arslan Kibria"
                        },
                        "dob": "1999-07-15",
                        "document_number": "3410130579153",
                        "gender": "M",
                        "expiry_date": "2032-12-18",
                        "issue_date": "2022-12-18",
                        "full_address": "Karachi, Pakistan",
                        "age": 26,
                        "selected_type": ["id_card"]
                      },
                      "additional_data": {
                        "proof": {
                          "country_code": "PK",
                          "country": "pakistan",
                          "country_native": "اسلامی جمہوریہ پاکستان",
                          "full_name_native": "مرزا مومن بیگ",
                          "guardian_name": "Mushtaq Ahmed",
                          "document_official_name": "Pakistan National Identity Card",
                          "document_country": "Pakistan"
                        }
                      }
                    }
                  }
                }
                """.formatted(referenceId);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult faceMatchResponse() {
        String referenceId = "mock-selfie-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String body = """
                {
                  "status": 200,
                  "message": "success",
                  "result": {
                    "data": {
                      "reference_id": "%s",
                      "similarity_status": "1",
                      "similarity_score": 0.92
                    }
                  }
                }
                """.formatted(referenceId);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult accessTokenResponse() {
        String body = """
                {
                  "status": 200,
                  "message": "success",
                  "result": {
                    "data": {
                      "token": "mock-facia-access-token-%s",
                      "expires_in": 3600
                    }
                  }
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse(String apiCode) {
        String body = """
                {
                  "status": 404,
                  "message": "Mock: Facia API %s not handled",
                  "result": { "data": {} }
                }
                """.formatted(apiCode);
        return new MockResponseResult(404, body, HEADERS);
    }
}
