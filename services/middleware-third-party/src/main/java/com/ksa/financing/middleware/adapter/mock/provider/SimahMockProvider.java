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
                    yield lookupResponse(apiCode);
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
                    }
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult consumerReport() {
        var body = """
                {
                    "MESSAGE": {
                        "ITEM": {
                            "RSP_REPORT": {
                                "RSP_CODE": "000",
                                "RSP_MSG": "Success",
                                "RSP_DATA": {
                                    "CUSTOMER_NAME": "Test Customer",
                                    "CUSTOMER_ID": "1234567890",
                                    "SCORE": "750",
                                    "RATING": "Good"
                                }
                            }
                        }
                    }
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult consumerV2() {
        var body = """
                {
                    "isSuccess": true,
                    "data": {
                        "customerId": "1234567890",
                        "score": 750,
                        "rating": "Good",
                        "reportDate": "%s"
                    }
                }
                """.formatted(LocalDate.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult consumerReview() {
        var body = """
                {
                    "reviewId": "REV-%s",
                    "status": "Reviewed",
                    "score": 750
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult consumerScore() {
        var body = """
                {
                    "score": 750,
                    "rating": "Good",
                    "lastUpdated": "%s"
                }
                """.formatted(LocalDate.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult salaryCertificate() {
        var body = """
                {
                    "certificateId": "CERT-%s",
                    "status": "Generated",
                    "basicSalary": 15000,
                    "totalSalary": 18000,
                    "employerName": "Test Company"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult negativeConsumer() {
        var body = """
                {
                    "hasNegativeRecords": false,
                    "totalDefaults": 0,
                    "status": "Clear"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult consumerAffordability() {
        var body = """
                {
                    "affordabilityId": "AFF-%s",
                    "status": "Assessed",
                    "maxMonthlyInstallment": 5000,
                    "dti": 35.5
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult creditCommitments() {
        var body = """
                {
                    "totalCommitments": 2,
                    "totalMonthlyPayment": 3500,
                    "totalOutstanding": 120000,
                    "commitments": []
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult miscellaneous() {
        var body = """
                {
                    "enquiryId": "ENQ-%s",
                    "status": "Completed",
                    "result": "Processed"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult lookupResponse(String apiCode) {
        var body = """
                {
                    "items": [
                        {"id": 1, "name": "Mock Item 1"},
                        {"id": 2, "name": "Mock Item 2"},
                        {"id": 3, "name": "Mock Item 3"}
                    ]
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "status": "Success",
                    "message": "SIMAH request processed"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }
}
