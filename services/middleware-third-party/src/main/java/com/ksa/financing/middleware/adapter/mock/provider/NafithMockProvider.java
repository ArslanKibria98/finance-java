package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class NafithMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "NAFITH";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "NAFITH_AUTH" -> authResponse();
            case "NAFITH_CREATE_SANAD", "NAFITH_REGISTER" -> createSanadGroup();
            case "NAFITH_SANAD_STATUS" -> sanadStatusResponse();
            case "NAFITH_DOWNLOAD_PDF" -> downloadPdfResponse();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult authResponse() {
        var body = """
                {
                    "access_token": "mock-nafith-token-%s",
                    "token_type": "Bearer",
                    "expires_in": 3600,
                    "scope": "sanad:create sanad:read sanad:download"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult createSanadGroup() {
        var groupId = UUID.randomUUID().toString();
        var startDate = LocalDate.now().plusMonths(1);
        var body = """
                {
                    "statusCode": "200",
                    "statusMessage": "Success",
                    "sanadGroup": {
                        "groupId": "%s",
                        "groupNumber": "SND-%s",
                        "status": "CREATED",
                        "sanadCount": 12,
                        "totalAmount": 120000.00,
                        "monthlyInstallment": 10000.00,
                        "currency": "SAR",
                        "creditor": {
                            "name": "\u0634\u0631\u0643\u0629 \u0639\u0648\u0646 \u0627\u0644\u0631\u0627\u0626\u062f\u0629 \u0644\u0644\u062a\u0645\u0648\u064a\u0644 \u0627\u0644\u0627\u0633\u062a\u0647\u0644\u0627\u0643\u064a \u0627\u0644\u0645\u0635\u063a\u0631",
                            "nameEn": "AWN Pioneering Micro Finance Company",
                            "crNumber": "7025558920",
                            "iban": "SA8030000000000000001234"
                        },
                        "debtor": {
                            "name": "\u0641\u064a\u0635\u0644 \u0647\u0644\u064a\u0644 \u0627\u0644\u0639\u062a\u064a\u0628\u064a",
                            "nameEn": "FAISAL HULAYYIL ALOTAIBI",
                            "nationalId": "1088052343",
                            "mobile": "966590933288"
                        },
                        "firstInstallmentDate": "%s",
                        "lastInstallmentDate": "%s",
                        "sanads": [
                            {
                                "sanadNumber": "SND-%s-01",
                                "amount": 10000.00,
                                "dueDate": "%s",
                                "status": "PENDING_SIGNATURE"
                            },
                            {
                                "sanadNumber": "SND-%s-02",
                                "amount": 10000.00,
                                "dueDate": "%s",
                                "status": "PENDING_SIGNATURE"
                            }
                        ],
                        "createdAt": "%s"
                    }
                }
                """.formatted(
                groupId,
                groupId.substring(0, 8),
                startDate.toString(),
                startDate.plusMonths(11).toString(),
                groupId.substring(0, 8), startDate.toString(),
                groupId.substring(0, 8), startDate.plusMonths(1).toString(),
                Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult sanadStatusResponse() {
        var body = """
                {
                    "statusCode": "200",
                    "statusMessage": "Success",
                    "groupId": "%s",
                    "status": "SIGNED",
                    "signedAt": "%s",
                    "signedBy": "1088052343",
                    "verificationMethod": "NAFATH",
                    "sanadCount": 12,
                    "totalAmount": 120000.00,
                    "allSigned": true
                }
                """.formatted(UUID.randomUUID().toString(), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult downloadPdfResponse() {
        var body = """
                {
                    "statusCode": "200",
                    "statusMessage": "Success",
                    "groupId": "%s",
                    "downloadUrl": "https://mock.nafith.sa/downloads/sanad-%s.pdf",
                    "fileSize": 245678,
                    "contentType": "application/pdf",
                    "expiresAt": "%s"
                }
                """.formatted(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString().substring(0, 8),
                Instant.now().plusSeconds(3600).toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "statusCode": "200",
                    "statusMessage": "Success",
                    "timestamp": "%s"
                }
                """.formatted(Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }
}
