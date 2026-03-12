package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class TarabutMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "TARABUT";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "TARABUT_AUTH" -> loginResponse();
            case "TARABUT_KSA_GET_PROVIDERS", "TARABUT_BAH_GET_PROVIDERS" -> providersResponse();
            case "TARABUT_KSA_CREATE_INTENT", "TARABUT_BAH_CREATE_INTENT" -> createIntentResponse();
            case "TARABUT_KSA_GET_INTENT" -> intentStatusResponse();
            case "TARABUT_KSA_GET_ACCOUNTS", "TARABUT_BAH_GET_ACCOUNTS" -> accountsResponse();
            case "TARABUT_KSA_GET_ACCOUNT_DETAILS" -> accountDetailsResponse();
            case "TARABUT_KSA_GET_BALANCES", "TARABUT_BAH_GET_BALANCES" -> balanceResponse();
            case "TARABUT_KSA_GET_TRANSACTIONS", "TARABUT_BAH_GET_TRANSACTIONS" -> transactionsResponse();
            case "TARABUT_KSA_REVOKE_CONSENT" -> revokeConsentResponse();
            case "TARABUT_KSA_CATEGORIZATION", "TARABUT_BAH_CATEGORIZATION" -> categoriseResponse();
            case "TARABUT_KSA_SALARY_CHECK", "TARABUT_BAH_SALARY_CHECK" -> salaryCheckResponse();
            case "TARABUT_KSA_ACCOUNT_VERIFY" -> accountVerifyResponse();
            case "TARABUT_KSA_IBAN_MATCH" -> ibanMatchResponse();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult loginResponse() {
        var body = """
                {
                    "access_token": "mock-tarabut-token-%s",
                    "token_type": "Bearer",
                    "expires_in": 3600
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult providersResponse() {
        var body = """
                {
                    "banks": [
                        {"bankId": "RJHI", "bankName": "Al Rajhi Bank", "country": "SA", "status": "ACTIVE"},
                        {"bankId": "SABB", "bankName": "Saudi British Bank", "country": "SA", "status": "ACTIVE"},
                        {"bankId": "ALBI", "bankName": "Bank Albilad", "country": "SA", "status": "ACTIVE"},
                        {"bankId": "ALIN", "bankName": "Alinma Bank", "country": "SA", "status": "ACTIVE"}
                    ]
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult createIntentResponse() {
        var body = """
                {
                    "intentId": "INT-%s",
                    "status": "CREATED",
                    "redirectUrl": "https://mock.tarabut.com/consent/%s",
                    "createdAt": "%s"
                }
                """.formatted(
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8),
                Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult intentStatusResponse() {
        var body = """
                {
                    "intentId": "INT-%s",
                    "status": "COMPLETED",
                    "consentStatus": "AUTHORIZED",
                    "bankId": "RJHI",
                    "createdAt": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult accountsResponse() {
        var body = """
                {
                    "accounts": [
                        {
                            "accountId": "ACC-%s",
                            "iban": "SA0380000000608010167519",
                            "currency": "SAR",
                            "accountType": "CURRENT",
                            "bankId": "RJHI"
                        },
                        {
                            "accountId": "ACC-%s",
                            "iban": "SA4420000001234567891234",
                            "currency": "SAR",
                            "accountType": "SAVINGS",
                            "bankId": "RJHI"
                        }
                    ]
                }
                """.formatted(
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult accountDetailsResponse() {
        var body = """
                {
                    "accountId": "ACC-%s",
                    "accountHolderName": "Mohammed Al-Test",
                    "iban": "SA0380000000608010167519",
                    "currency": "SAR",
                    "accountType": "CURRENT",
                    "bankId": "RJHI",
                    "status": "ACTIVE"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult balanceResponse() {
        var body = """
                {
                    "accountId": "ACC-%s",
                    "balance": 25000.00,
                    "currency": "SAR",
                    "balanceType": "AVAILABLE",
                    "lastUpdated": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult transactionsResponse() {
        var body = """
                {
                    "transactions": [
                        {
                            "transactionId": "TXN-%s",
                            "amount": 5000.00,
                            "currency": "SAR",
                            "type": "CREDIT",
                            "description": "Salary Transfer",
                            "date": "2026-03-01"
                        },
                        {
                            "transactionId": "TXN-%s",
                            "amount": 1200.00,
                            "currency": "SAR",
                            "type": "DEBIT",
                            "description": "Rent Payment",
                            "date": "2026-03-02"
                        },
                        {
                            "transactionId": "TXN-%s",
                            "amount": 350.00,
                            "currency": "SAR",
                            "type": "DEBIT",
                            "description": "Utility Bill",
                            "date": "2026-03-03"
                        }
                    ]
                }
                """.formatted(
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult revokeConsentResponse() {
        var body = """
                {
                    "intentId": "INT-%s",
                    "status": "REVOKED",
                    "revokedAt": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult categoriseResponse() {
        var body = """
                {
                    "categorisedTransactions": [
                        {
                            "transactionId": "TXN-%s",
                            "category": "INCOME",
                            "subCategory": "SALARY",
                            "confidence": 0.95
                        },
                        {
                            "transactionId": "TXN-%s",
                            "category": "HOUSING",
                            "subCategory": "RENT",
                            "confidence": 0.90
                        },
                        {
                            "transactionId": "TXN-%s",
                            "category": "UTILITIES",
                            "subCategory": "ELECTRICITY",
                            "confidence": 0.85
                        }
                    ],
                    "summary": {
                        "totalIncome": 5000.00,
                        "totalExpenses": 1550.00,
                        "netFlow": 3450.00
                    }
                }
                """.formatted(
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult salaryCheckResponse() {
        var body = """
                {
                    "verified": true,
                    "monthlySalary": 15000.00,
                    "currency": "SAR",
                    "employerName": "Test Company Ltd",
                    "lastSalaryDate": "2026-03-01",
                    "consecutiveMonths": 6
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult accountVerifyResponse() {
        var body = """
                {
                    "verified": true,
                    "accountHolderName": "Mohammed Al-Test",
                    "iban": "SA0380000000608010167519",
                    "bankId": "RJHI",
                    "verifiedAt": "%s"
                }
                """.formatted(Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult ibanMatchResponse() {
        var body = """
                {
                    "matched": true,
                    "iban": "SA0380000000608010167519",
                    "accountHolderName": "Mohammed Al-Test",
                    "matchScore": 0.98,
                    "verifiedAt": "%s"
                }
                """.formatted(Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "status": "Success",
                    "message": "TARABUT request processed"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }
}
