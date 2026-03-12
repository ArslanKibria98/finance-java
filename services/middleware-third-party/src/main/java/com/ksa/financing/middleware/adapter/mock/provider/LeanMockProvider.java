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
            default -> fallbackResponse();
        };
    }

    private MockResponseResult createEntityResponse() {
        var body = """
                {
                    "entityId": "ENT-%s",
                    "status": "CREATED",
                    "iban": "SA0380000000608010167519",
                    "accountHolderName": "Mohammed Al-Test",
                    "bankName": "Al Rajhi Bank",
                    "bankCode": "RJHI",
                    "createdAt": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult getAccountsResponse() {
        var body = """
                {
                    "accounts": [
                        {
                            "accountId": "ACC-%s",
                            "iban": "SA0380000000608010167519",
                            "currency": "SAR",
                            "accountType": "CURRENT",
                            "bankId": "RJHI",
                            "balance": 25000.00
                        }
                    ]
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult getTransactionsResponse() {
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
                        }
                    ]
                }
                """.formatted(
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "status": "Success",
                    "message": "LEAN request processed"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }
}
