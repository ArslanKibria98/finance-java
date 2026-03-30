package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class SimahMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "SIMAH";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "SIMAH_LOGIN" -> loginResponse();
            case "SIMAH_CONSUMER_REPORT", "SIMAH_ENQUIRY_NEW_V1" -> consumerReport();
            case "SIMAH_ENQUIRY_NEW_V2" -> consumerV2();
            case "SIMAH_ENQUIRY_REVIEW" -> consumerReview();
            case "SIMAH_SCORE_CREDIT", "SIMAH_SCORE_CREDIT_V2" -> consumerScore();
            case "SIMAH_SALARY_CERTIFICATE", "SIMAH_SALARY_CERTIFICATE_MOF" -> salaryCertificate();
            case "SIMAH_NEGATIVE_CONSUMER" -> negativeConsumer();
            case "SIMAH_CONSUMER_AFFORDABILITY" -> consumerAffordability();
            case "SIMAH_CREDIT_COMMITMENTS" -> creditCommitments();
            case "SIMAH_MISCELLANEOUS" -> miscellaneous();
            default -> {
                if (apiCode.startsWith("SIMAH_LOOKUP_")) {
                    yield lookupResponse();
                }
                yield fallbackResponse();
            }
        };
    }

    private MockResponseResult loginResponse() {
        var body = """
                {
                    "isSuccess": true,
                    "data": {
                        "token": "mock-simah-token-%s"
                    },
                    "message": null,
                    "errorCode": null
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult consumerReport() {
        var refNum = "ALAN-" + UUID.randomUUID().toString().substring(0, 10).replaceAll("-", "");
        var body = """
                {
                    "data": {
                        "productType": 155,
                        "amount": 1000,
                        "applicants": [
                            {
                                "identityInfo": {
                                    "idType": 1,
                                    "idNumber": "1108149475"
                                },
                                "demographicInfo": {
                                    "isHijriIDExpiryDate": false,
                                    "idExpiryDate": "28/11/2028",
                                    "nationality": 196,
                                    "maritalStatus": 2,
                                    "isHijriDateOfBirth": false,
                                    "dateOfBirth": "29/09/1999",
                                    "firstName": "FAISAL",
                                    "gender": 1,
                                    "secondName": "HULAYYIL",
                                    "thirdName": "OBAID",
                                    "familyName": "OBAID",
                                    "applicantType": 1,
                                    "totalMonthlyIncome": 9259,
                                    "mobileNumber": "590933288",
                                    "city": 333
                                }
                            }
                        ],
                        "accept": true,
                        "referenceNumber": "%s"
                    },
                    "message": "GEN_000",
                    "errorCode": "200",
                    "isSuccess": true
                }
                """.formatted(refNum);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult consumerV2() {
        var body = """
                {
                    "isSuccess": true,
                    "data": {
                        "customerId": "1108149475",
                        "score": 750,
                        "rating": "Good",
                        "reportDate": "%s"
                    },
                    "message": "GEN_000",
                    "errorCode": "200"
                }
                """.formatted(LocalDate.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult consumerReview() {
        var body = """
                {
                    "isSuccess": true,
                    "data": {
                        "reviewId": "REV-%s",
                        "status": "Reviewed",
                        "score": 750
                    },
                    "message": "GEN_000",
                    "errorCode": "200"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult consumerScore() {
        var body = """
                {
                    "isSuccess": true,
                    "data": {
                        "score": 750,
                        "rating": "Good",
                        "lastUpdated": "%s"
                    },
                    "message": "GEN_000",
                    "errorCode": "200"
                }
                """.formatted(LocalDate.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult salaryCertificate() {
        var body = """
                {
                    "isSuccess": true,
                    "data": {
                        "certificateId": "CERT-%s",
                        "status": "Generated",
                        "basicSalary": 7408,
                        "totalSalary": 9259,
                        "employerName": "\u0634\u0631\u0643\u0629 \u0639\u0648\u0646 \u0627\u0644\u0631\u0627\u0626\u062f\u0629 \u0644\u0644\u062a\u0645\u0648\u064a\u0644 \u0627\u0644\u0627\u0633\u062a\u0647\u0644\u0627\u0643\u064a \u0627\u0644\u0645\u0635\u063a\u0631"
                    },
                    "message": "GEN_000",
                    "errorCode": "200"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult negativeConsumer() {
        var body = """
                {
                    "isSuccess": true,
                    "data": {
                        "hasNegativeRecords": false,
                        "totalDefaults": 0,
                        "status": "Clear"
                    },
                    "message": "GEN_000",
                    "errorCode": "200"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult consumerAffordability() {
        var body = """
                {
                    "isSuccess": true,
                    "data": {
                        "affordabilityId": "AFF-%s",
                        "status": "Assessed",
                        "maxMonthlyInstallment": 5000,
                        "dti": 35.5
                    },
                    "message": "GEN_000",
                    "errorCode": "200"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult creditCommitments() {
        var body = """
                {
                    "isSuccess": true,
                    "data": {
                        "totalCommitments": 2,
                        "totalMonthlyPayment": 3500,
                        "totalOutstanding": 120000,
                        "commitments": []
                    },
                    "message": "GEN_000",
                    "errorCode": "200"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult miscellaneous() {
        var body = """
                {
                    "isSuccess": true,
                    "data": {
                        "enquiryId": "ENQ-%s",
                        "status": "Completed",
                        "result": "Processed"
                    },
                    "message": "GEN_000",
                    "errorCode": "200"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult lookupResponse() {
        var body = """
                {
                    "isSuccess": true,
                    "data": {
                        "items": [
                            {"id": 1, "name": "Mock Item 1"},
                            {"id": 2, "name": "Mock Item 2"},
                            {"id": 3, "name": "Mock Item 3"}
                        ]
                    },
                    "message": "GEN_000",
                    "errorCode": "200"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "data": null,
                    "message": "GEN_000",
                    "errorCode": "200",
                    "isSuccess": true
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }
}
