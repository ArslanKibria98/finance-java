package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
            case "EIGER_ORDER_PURCHASE" -> orderPurchase();
            case "EIGER_ORDER_PURCHASE_UPDATE" -> orderPurchaseUpdate();
            case "EIGER_TRANSFER_NOTIFICATION" -> transferNotification();
            case "EIGER_ORDER_SALE" -> orderSale();
            case "EIGER_ORDER_SALE_UPDATE" -> orderSaleUpdate();
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

    private MockResponseResult orderPurchase() {
        var body = """
                {
                    "order_id": "ORD-%s",
                    "status": "PURCHASED",
                    "commodity": "Palladium",
                    "quantity": 10,
                    "unitPrice": 1250.00,
                    "totalValue": 12500.00,
                    "purchaseDate": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult orderPurchaseUpdate() {
        var body = """
                {
                    "order_id": "ORD-%s",
                    "status": "PURCHASE_UPDATED",
                    "commodity": "Palladium",
                    "quantity": 10,
                    "unitPrice": 1255.00,
                    "totalValue": 12550.00,
                    "updatedAt": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult transferNotification() {
        var body = """
                {
                    "order_id": "ORD-%s",
                    "status": "TRANSFERRED",
                    "commodity": "Palladium",
                    "quantity": 10,
                    "transferReference": "TRF-%s",
                    "transferDate": "%s"
                }
                """.formatted(
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8),
                Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult orderSale() {
        var body = """
                {
                    "order_id": "ORD-%s",
                    "status": "SALE_REQUESTED",
                    "commodity": "Palladium",
                    "quantity": 10,
                    "requestedPrice": 1260.00,
                    "requestDate": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult orderSaleUpdate() {
        var body = """
                {
                    "order_id": "ORD-%s",
                    "status": "SALE_CONFIRMED",
                    "commodity": "Palladium",
                    "quantity": 10,
                    "salePrice": 1260.00,
                    "totalProceeds": 12600.00,
                    "confirmationDate": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "status": "Success",
                    "message": "EIGER request processed"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }
}
