package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sullis KYC mock provider — used when the middleware is hit by a TEST client.
 *
 * <p>Sullis returns <b>flat JSON</b> (no {@code result.data} envelope), so the
 * shapes below mirror the live Sullis responses verbatim. The {@code submit}
 * step returns an <b>APPROVED</b> outcome so the onboarding happy-path passes
 * in TEST (the live sandbox declined the sample doc on MRZ).</p>
 *
 * <p>Document number / session / attempt / upload ids are randomised per call
 * so repeated TEST onboarding runs do not collide on a duplicate NID
 * (the same trap that bit the Facia mock — see memory onboarding_facia_mock_nid).</p>
 */
@Component
public class SullisMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "SULLIS";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "SULLIS_CREATE_SESSION" -> createSessionResponse();
            case "SULLIS_START_ATTEMPT"  -> startAttemptResponse();
            case "SULLIS_UPLOAD_DOCUMENT" -> uploadDocumentResponse();
            case "SULLIS_UPLOAD_SELFIE"  -> uploadSelfieResponse();
            case "SULLIS_SUBMIT"         -> submitResponse();
            default -> fallbackResponse(apiCode);
        };
    }

    private MockResponseResult createSessionResponse() {
        String sessionId = UUID.randomUUID().toString();
        String body = """
                {
                  "sessionId": "%s",
                  "tenantId": "fcede514-ad91-4016-a415-ac3c381a52cf",
                  "customerReference": "mock-cust-%s",
                  "status": "CREATED",
                  "attemptCount": 0,
                  "maxAttempts": 3,
                  "expiresAt": "2099-12-31T23:59:59Z",
                  "createdAt": "2026-01-01T00:00:00Z",
                  "completedAt": null,
                  "declineReason": null,
                  "sdkToken": "mock-sdk-token-%s"
                }
                """.formatted(sessionId, shortId(), shortId());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult startAttemptResponse() {
        String attemptId = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();
        String body = """
                {
                  "id": "%s",
                  "sessionId": "%s",
                  "attemptNumber": 1,
                  "outcome": "IN_PROGRESS",
                  "riskScore": null,
                  "failureReason": null,
                  "startedAt": "2026-01-01T00:00:00Z",
                  "completedAt": null
                }
                """.formatted(attemptId, sessionId);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult uploadDocumentResponse() {
        // Unique national/document number per run so re-tests don't collide on duplicate NID.
        long docNumber = ThreadLocalRandom.current().nextLong(1_000_000_000_00L, 9_999_999_999_99L);
        // Real Sullis upload-document returns OCR under `ocr` (snake_case).
        String body = """
                {
                  "uploadId": "%s",
                  "sessionId": "%s",
                  "attemptId": "%s",
                  "storageKey": "documents/mock/%s.jpeg",
                  "sizeBytes": 96826,
                  "sha256": "%s",
                  "ocr": {
                    "document_type": "PASSPORT",
                    "extraction_status": "EXTRACTED",
                    "reason": null,
                    "document_number": "%d",
                    "full_name": "Arslan Kibria",
                    "given_names": "Arslan",
                    "surname": "Kibria",
                    "date_of_birth": "1999-07-15",
                    "expiry_date": "2032-12-18",
                    "nationality": "PAK",
                    "sex": "M",
                    "issuing_country": "PAK",
                    "mrz_valid": true,
                    "ocr_score": 95,
                    "mrz_score": 100,
                    "is_expired": false,
                    "days_to_expiry": 2450
                  }
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                              shortId(), sha256(), docNumber);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult uploadSelfieResponse() {
        String body = """
                {
                  "uploadId": "%s",
                  "sessionId": "%s",
                  "attemptId": "%s",
                  "storageKey": "biometrics/mock/%s.jpeg",
                  "sizeBytes": 137003,
                  "sha256": "%s"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                              shortId(), sha256());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult submitResponse() {
        long docNumber = ThreadLocalRandom.current().nextLong(1_000_000_000_00L, 9_999_999_999_99L);
        // Real Sullis submit: outcome "VERIFIED" + `extracted` (camelCase) + scores.
        String body = """
                {
                  "sessionId": "%s",
                  "attemptId": "%s",
                  "extracted": {
                    "documentType": "PASSPORT",
                    "documentNumber": "%d",
                    "fullName": "Arslan Kibria",
                    "dateOfBirth": "1999-07-15",
                    "expiryDate": "2032-12-18",
                    "nationality": "PAK",
                    "issuingCountry": "PAK",
                    "mrzValid": true,
                    "isExpired": false,
                    "daysToExpiry": 2450
                  },
                  "scores": {
                    "ocr": 95,
                    "mrz": 100,
                    "documentLiveness": 92,
                    "faceLiveness": 99,
                    "faceMatch": 95,
                    "nfc": null,
                    "faceSamePerson": true
                  },
                  "outcome": "VERIFIED",
                  "riskScore": 12,
                  "reason": null
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), docNumber);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse(String apiCode) {
        String body = """
                {
                  "error": "Mock: Sullis API %s not handled"
                }
                """.formatted(apiCode);
        return new MockResponseResult(404, body, HEADERS);
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String sha256() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }
}
