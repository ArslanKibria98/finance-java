package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EigerMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "EIGER";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "EIGER_AUTH" -> authResponse();
            case "EIGER_ORDER_HOLD" -> orderHold();
            case "EIGER_ORDER_PURCHASE", "EIGER_COMMODITY_BUY" -> orderPurchase();
            case "EIGER_ORDER_PURCHASE_UPDATE" -> orderPurchase();
            case "EIGER_TRANSFER_NOTIFICATION" -> transferNotification();
            case "EIGER_ORDER_SALE", "EIGER_COMMODITY_SELL" -> orderSalePost();
            case "EIGER_ORDER_SALE_UPDATE" -> orderSalePatch();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult authResponse() {
        var body = """
                {
                    "access_token": "mock-eiger-token-%s",
                    "token_type": "Bearer",
                    "expires_in": 3600
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult orderHold() {
        var body = """
                {
                    "result": "Sale Requested",
                    "resultDescription": "OK",
                    "eigerResultCode": 200
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult orderPurchase() {
        var body = """
                {
                    "result": "Purchased",
                    "resultDescription": "OK",
                    "eigerResultCode": 200
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult transferNotification() {
        var body = """
                {
                    "result": "Transferred",
                    "resultDescription": "OK",
                    "eigerResultCode": 200
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult orderSalePost() {
        var body = """
                {
                    "result": "Sale Requested",
                    "resultDescription": "OK",
                    "eigerResultCode": 200
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult orderSalePatch() {
        var body = """
                {
                    "result": "Sold",
                    "resultDescription": "OK",
                    "eigerResultCode": 200
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "result": "OK",
                    "resultDescription": "OK",
                    "eigerResultCode": 200
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }
}
