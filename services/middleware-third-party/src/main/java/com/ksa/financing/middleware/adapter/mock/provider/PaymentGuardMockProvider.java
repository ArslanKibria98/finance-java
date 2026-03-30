package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

@Component
public class PaymentGuardMockProvider implements MockResponseProvider {

    private static final String XML_HEADERS = "Content-Type: application/xml";

    @Override
    public String getProviderCode() {
        return "PAYMENT_GUARD";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "PAYMENT_GUARD_ACCOUNT_REGISTRATION" -> accountRegistration();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult accountRegistration() {
        var body = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>\
                <message>\
                <status-code>OK</status-code>\
                <status-code-id>200</status-code-id>\
                <status-msg>Message Added</status-msg>\
                </message>""";
        return new MockResponseResult(200, body, XML_HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>\
                <message>\
                <status-code>OK</status-code>\
                <status-code-id>200</status-code-id>\
                <status-msg>Request Processed</status-msg>\
                </message>""";
        return new MockResponseResult(200, body, XML_HEADERS);
    }
}
