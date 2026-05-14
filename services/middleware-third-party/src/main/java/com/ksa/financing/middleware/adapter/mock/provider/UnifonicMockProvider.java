package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UnifonicMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "UNIFONIC";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "UNIFONIC_SEND_OTP", "UNIFONIC_OTP_SEND" -> smsResponse();
            case "UNIFONIC_OTP_VERIFY" -> otpVerifyResponse();
            case "UNIFONIC_IVR", "UNIFONIC_IVR_CALL" -> ivrInitiate();
            case "UNIFONIC_IVR_STATUS" -> ivrStatus();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult smsResponse() {
        var body = """
                {
                    "Status": "Sent",
                    "MessageId": "MSG-%s",
                    "Recipient": "966590933288"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult ivrInitiate() {
        var callId = UUID.randomUUID().toString();
        var body = """
                {
                    "status": "Request has been sent.",
                    "callId": "%s"
                }
                """.formatted(callId);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult otpVerifyResponse() {
        var body = """
                {
                    "Status": "Verified",
                    "MessageId": "MSG-%s",
                    "verified": true
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult ivrStatus() {
        var body = """
                {
                    "callId": "%s",
                    "status": "Completed",
                    "duration": 45
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "status": "Request has been sent.",
                    "callId": "%s"
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }
}
