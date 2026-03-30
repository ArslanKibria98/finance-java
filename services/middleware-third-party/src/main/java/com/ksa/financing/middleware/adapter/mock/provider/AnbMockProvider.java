package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class AnbMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "ANB";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "ANB_AUTH" -> authResponse();
            case "ANB_TRANSFER" -> transferResponse();
            case "ANB_TRANSFER_STATUS" -> transferStatusResponse();
            case "ANB_ACCOUNT_INQUIRY" -> accountInquiryResponse();
            case "ANB_BALANCE_INQUIRY" -> balanceInquiryResponse();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult authResponse() {
        var body = """
                {
                    "access_token": "mock-anb-token-%s",
                    "token_type": "Bearer",
                    "expires_in": 3600,
                    "scope": "transfer:write account:read"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult transferResponse() {
        var paymentId = UUID.randomUUID().toString();
        var body = """
                {
                    "responseCode": "000",
                    "responseMessage": "Successful",
                    "paymentId": "%s",
                    "transactionRef": "ANB-%s",
                    "status": "ACCEPTED",
                    "amount": 10000.00,
                    "currency": "SAR",
                    "beneficiaryIban": "SA2999990000000000004621",
                    "beneficiaryName": "FAISAL HULAYYIL ALOTAIBI",
                    "beneficiaryBankCode": "ARNBSARI",
                    "debitAccountIban": "SA8030000000000000001234",
                    "valueDate": "%s",
                    "channel": "API",
                    "purposeOfTransfer": "LOAN_DISBURSEMENT",
                    "timestamp": "%s"
                }
                """.formatted(paymentId, paymentId.substring(0, 8),
                LocalDate.now().toString(), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult transferStatusResponse() {
        var body = """
                {
                    "responseCode": "000",
                    "responseMessage": "Successful",
                    "paymentId": "%s",
                    "status": "COMPLETED",
                    "completedAt": "%s",
                    "sariReference": "SARI%s"
                }
                """.formatted(UUID.randomUUID().toString(),
                Instant.now().toString(),
                UUID.randomUUID().toString().substring(0, 12).replaceAll("-", ""));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult accountInquiryResponse() {
        var body = """
                {
                    "responseCode": "000",
                    "responseMessage": "Successful",
                    "account": {
                        "accountNumber": "0000000000001234",
                        "iban": "SA8030000000000000001234",
                        "accountType": "CURRENT",
                        "currency": "SAR",
                        "status": "ACTIVE",
                        "accountHolderName": "\u0634\u0631\u0643\u0629 \u0639\u0648\u0646 \u0627\u0644\u0631\u0627\u0626\u062f\u0629 \u0644\u0644\u062a\u0645\u0648\u064a\u0644 \u0627\u0644\u0627\u0633\u062a\u0647\u0644\u0627\u0643\u064a \u0627\u0644\u0645\u0635\u063a\u0631",
                        "accountHolderNameEn": "AWN Pioneering Micro Finance Company",
                        "branchCode": "37040044",
                        "openDate": "2023-01-15"
                    }
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult balanceInquiryResponse() {
        var body = """
                {
                    "responseCode": "000",
                    "responseMessage": "Successful",
                    "accountNumber": "0000000000001234",
                    "iban": "SA8030000000000000001234",
                    "currency": "SAR",
                    "availableBalance": 2500000.00,
                    "currentBalance": 2500000.00,
                    "unclearedBalance": 0.00,
                    "lastTransactionDate": "%s",
                    "retrievedAt": "%s"
                }
                """.formatted(LocalDate.now().minusDays(1).toString(), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "responseCode": "000",
                    "responseMessage": "Successful",
                    "timestamp": "%s"
                }
                """.formatted(Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }
}
