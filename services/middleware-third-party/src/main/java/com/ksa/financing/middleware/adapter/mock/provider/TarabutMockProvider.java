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
        var now = Instant.now().toString();
        var body = """
                {
                    "accounts": [
                        {
                            "accountId": "c0df3c2f-b304-31b1-ad67-f5ec726ebf37",
                            "accountHolderName": "John Doe",
                            "accountProductType": "CreditCard",
                            "accountDescription": "Mustafa Khalid - SAR Credit Card (thin file)",
                            "providerId": "BLUE",
                            "lastUpdatedDateTime": "%s",
                            "consents": [
                                {
                                    "id": "665f1f68-3fa0-463c-aff3-cbe57ac249b3",
                                    "expiryDate": "2026-06-17T07:35:51Z",
                                    "status": "ACTIVE"
                                }
                            ],
                            "identifiers": [
                                {"type": "maskedPAN", "value": "******************1846"}
                            ],
                            "balances": [
                                {"type": "Unknown", "amount": {"value": 1.3, "currency": "SAR"}}
                            ],
                            "meta": {
                                "lastAccountUpdateDatetime": "%s",
                                "lastBalancesUpdateDatetime": "%s",
                                "transactionsAvailability": "progressing"
                            }
                        },
                        {
                            "accountId": "5053cbd4-a32b-301d-b17f-aeb7ebc637aa",
                            "accountHolderName": "John Doe",
                            "accountProductType": "CreditCard",
                            "accountDescription": "John Doe - SAR Credit Card",
                            "providerId": "BLUE",
                            "lastUpdatedDateTime": "%s",
                            "consents": [
                                {
                                    "id": "665f1f68-3fa0-463c-aff3-cbe57ac249b3",
                                    "expiryDate": "2026-06-17T07:35:51Z",
                                    "status": "ACTIVE"
                                }
                            ],
                            "identifiers": [
                                {"type": "maskedPAN", "value": "******************4701"}
                            ],
                            "balances": [
                                {"type": "Unknown", "amount": {"value": 100, "currency": "SAR"}}
                            ],
                            "meta": {
                                "lastAccountUpdateDatetime": "%s",
                                "lastBalancesUpdateDatetime": "%s",
                                "transactionsAvailability": "progressing"
                            }
                        },
                        {
                            "accountId": "9f556dd9-43ff-3569-b701-357236b21ed9",
                            "accountHolderName": "John Doe",
                            "accountProductType": "Savings",
                            "accountDescription": "John Doe - SAR Super Saver",
                            "providerId": "BLUE",
                            "lastUpdatedDateTime": "%s",
                            "consents": [
                                {
                                    "id": "665f1f68-3fa0-463c-aff3-cbe57ac249b3",
                                    "expiryDate": "2026-06-17T07:35:51Z",
                                    "status": "ACTIVE"
                                }
                            ],
                            "identifiers": [
                                {"type": "maskedPAN", "value": "******************4942"}
                            ],
                            "balances": [
                                {"type": "Unknown", "amount": {"value": 100, "currency": "SAR"}}
                            ],
                            "meta": {
                                "lastAccountUpdateDatetime": "%s",
                                "lastBalancesUpdateDatetime": "%s",
                                "transactionsAvailability": "progressing"
                            }
                        },
                        {
                            "accountId": "ee229694-9a70-35da-ab0c-198fce6fe639",
                            "accountHolderName": "John Doe",
                            "accountProductType": "CurrentAccount",
                            "accountDescription": "John Doe - SAR Current",
                            "providerId": "BLUE",
                            "lastUpdatedDateTime": "%s",
                            "consents": [
                                {
                                    "id": "665f1f68-3fa0-463c-aff3-cbe57ac249b3",
                                    "expiryDate": "2026-06-17T07:35:51Z",
                                    "status": "ACTIVE"
                                }
                            ],
                            "identifiers": [
                                {"type": "IBAN", "value": "SA2999990000000000004621"}
                            ],
                            "balances": [
                                {"type": "Unknown", "amount": {"value": 3270, "currency": "SAR"}}
                            ],
                            "meta": {
                                "lastAccountUpdateDatetime": "%s",
                                "lastBalancesUpdateDatetime": "%s",
                                "transactionsAvailability": "progressing"
                            }
                        },
                        {
                            "accountId": "6549b0dd-27a1-3f5b-ab2a-9a83d8245d16",
                            "accountHolderName": "John Doe",
                            "accountProductType": "Savings",
                            "accountDescription": "John Doe - SAR Savings",
                            "providerId": "BLUE",
                            "lastUpdatedDateTime": "%s",
                            "consents": [
                                {
                                    "id": "665f1f68-3fa0-463c-aff3-cbe57ac249b3",
                                    "expiryDate": "2026-06-17T07:35:51Z",
                                    "status": "ACTIVE"
                                }
                            ],
                            "identifiers": [
                                {"type": "IBAN", "value": "BH62BLUE00200000008527"}
                            ],
                            "balances": [
                                {"type": "Unknown", "amount": {"value": 1.3, "currency": "SAR"}}
                            ],
                            "meta": {
                                "lastAccountUpdateDatetime": "%s",
                                "lastBalancesUpdateDatetime": "%s",
                                "transactionsAvailability": "progressing"
                            }
                        }
                    ]
                }
                """.formatted(now, now, now, now, now, now, now, now, now, now, now, now, now, now, now);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult accountDetailsResponse() {
        var body = """
                {
                    "accountId": "ee229694-9a70-35da-ab0c-198fce6fe639",
                    "accountHolderName": "John Doe",
                    "accountProductType": "CurrentAccount",
                    "accountDescription": "John Doe - SAR Current",
                    "providerId": "BLUE",
                    "identifiers": [
                        {"type": "IBAN", "value": "SA2999990000000000004621"}
                    ],
                    "balances": [
                        {"type": "Unknown", "amount": {"value": 3270, "currency": "SAR"}}
                    ]
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult balanceResponse() {
        var body = """
                {
                    "accountId": "ee229694-9a70-35da-ab0c-198fce6fe639",
                    "balances": [
                        {"type": "Unknown", "amount": {"value": 3270, "currency": "SAR"}}
                    ],
                    "lastUpdated": "%s"
                }
                """.formatted(Instant.now().toString());
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
                UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult salaryCheckResponse() {
        var body = """
                {
                    "verified": true,
                    "monthlySalary": 9259.00,
                    "currency": "SAR",
                    "employerName": "\u0634\u0631\u0643\u0629 \u0639\u0648\u0646 \u0627\u0644\u0631\u0627\u0626\u062f\u0629 \u0644\u0644\u062a\u0645\u0648\u064a\u0644 \u0627\u0644\u0627\u0633\u062a\u0647\u0644\u0627\u0643\u064a \u0627\u0644\u0645\u0635\u063a\u0631",
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
                    "accountHolderName": "John Doe",
                    "iban": "SA2999990000000000004621",
                    "bankId": "BLUE",
                    "verifiedAt": "%s"
                }
                """.formatted(Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult ibanMatchResponse() {
        var body = """
                {
                    "matched": true,
                    "iban": "SA2999990000000000004621",
                    "accountHolderName": "John Doe",
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
