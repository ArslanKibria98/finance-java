package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class LeanMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "LEAN";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "LEAN_CREATE_ENTITY" -> createEntityResponse();
            case "LEAN_GET_ACCOUNTS" -> getAccountsResponse();
            case "LEAN_GET_TRANSACTIONS" -> getTransactionsResponse();
            case "LEAN_GET_IDENTITY" -> getIdentityResponse();
            case "LEAN_GET_BALANCE" -> getBalanceResponse();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult createEntityResponse() {
        var entityId = UUID.randomUUID().toString();
        var body = """
                {
                    "type": "entity",
                    "entity_id": "%s",
                    "app_user_id": "customer-%s",
                    "permissions": ["identity", "accounts", "transactions", "balance"],
                    "bank_identifier": "RJHI_SAU_PERSONAL",
                    "bank_name": "Al Rajhi Bank",
                    "country": "SAU",
                    "status": "ACTIVE",
                    "created_at": "%s"
                }
                """.formatted(entityId, UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult getAccountsResponse() {
        var body = """
                {
                    "type": "accounts",
                    "accounts": [
                        {
                            "account_id": "%s",
                            "account_name": "Current Account",
                            "account_type": "CURRENT",
                            "iban": "SA2999990000000000004621",
                            "account_number": "000000004621",
                            "currency": "SAR",
                            "bank_identifier": "RJHI_SAU_PERSONAL",
                            "status": "ACTIVE"
                        },
                        {
                            "account_id": "%s",
                            "account_name": "Savings Account",
                            "account_type": "SAVINGS",
                            "iban": "SA4420000001234567891234",
                            "account_number": "001234567891234",
                            "currency": "SAR",
                            "bank_identifier": "RJHI_SAU_PERSONAL",
                            "status": "ACTIVE"
                        }
                    ]
                }
                """.formatted(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult getTransactionsResponse() {
        var body = """
                {
                    "type": "transactions",
                    "transactions": [
                        {
                            "transaction_id": "%s",
                            "amount": 9259.00,
                            "currency": "SAR",
                            "direction": "CREDIT",
                            "description": "Salary Transfer - \u0634\u0631\u0643\u0629 \u0639\u0648\u0646 \u0627\u0644\u0631\u0627\u0626\u062f\u0629",
                            "category": "INCOME",
                            "date": "2026-03-01T00:00:00Z",
                            "status": "BOOKED",
                            "balance_after": 15430.50
                        },
                        {
                            "transaction_id": "%s",
                            "amount": 2500.00,
                            "currency": "SAR",
                            "direction": "DEBIT",
                            "description": "Rent Payment",
                            "category": "HOUSING",
                            "date": "2026-03-02T00:00:00Z",
                            "status": "BOOKED",
                            "balance_after": 12930.50
                        },
                        {
                            "transaction_id": "%s",
                            "amount": 450.00,
                            "currency": "SAR",
                            "direction": "DEBIT",
                            "description": "SEC - Electricity Bill",
                            "category": "UTILITIES",
                            "date": "2026-03-05T00:00:00Z",
                            "status": "BOOKED",
                            "balance_after": 12480.50
                        },
                        {
                            "transaction_id": "%s",
                            "amount": 1200.00,
                            "currency": "SAR",
                            "direction": "DEBIT",
                            "description": "SADAD - Loan Installment",
                            "category": "LOAN_REPAYMENT",
                            "date": "2026-03-10T00:00:00Z",
                            "status": "BOOKED",
                            "balance_after": 11280.50
                        }
                    ],
                    "next_cursor": null
                }
                """.formatted(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult getIdentityResponse() {
        var body = """
                {
                    "type": "identity",
                    "identity": {
                        "full_name": "FAISAL HULAYYIL ALOTAIBI",
                        "full_name_local": "\u0641\u064a\u0635\u0644 \u0647\u0644\u064a\u0644 \u0627\u0644\u0639\u062a\u064a\u0628\u064a",
                        "date_of_birth": "1999-09-29",
                        "gender": "MALE",
                        "nationality": "SA",
                        "id_type": "NATIONAL_ID",
                        "id_number": "1088052343",
                        "phone_number": "+966590933288",
                        "email": null,
                        "address": {
                            "line1": "4302 Prince Mohammed Street",
                            "district": "Al Narjis Dist.",
                            "city": "Riyadh",
                            "postal_code": "13327",
                            "country": "SA"
                        }
                    }
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult getBalanceResponse() {
        var body = """
                {
                    "type": "balance",
                    "balance": {
                        "account_id": "%s",
                        "available": 11280.50,
                        "current": 11280.50,
                        "currency": "SAR",
                        "updated_at": "%s"
                    }
                }
                """.formatted(UUID.randomUUID().toString(), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "type": "success",
                    "status": "OK",
                    "timestamp": "%s"
                }
                """.formatted(Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }
}
