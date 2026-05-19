package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nafath mock with per-session-deterministic display names.
 *
 * On every {@code NAFATH_INITIATE} we pick a (firstName, thirdName) pair from
 * {@link RecipientNamePool} and cache it against the generated {@code transId}.
 * Subsequent {@code NAFATH_CHECK_STATUS} calls within the same session reuse
 * the cached pair, so a customer's name never changes mid-onboarding. The
 * merged "&lt;firstName&gt; &lt;thirdName&gt;" then flows downstream to
 * customer-service → Keycloak → wallet-service, giving every API a single
 * source of truth.
 */
@Component
public class NafathMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";
    private static final Pattern TRANS_ID_PATTERN = Pattern.compile("\"transId\"\\s*:\\s*\"([^\"]+)\"");

    private record NamePair(String first, String third) {}

    /** transId → picked (firstName, thirdName). Bounded by onboarding session count (mock; demo only). */
    private final ConcurrentHashMap<String, NamePair> sessionNames = new ConcurrentHashMap<>();

    @Override
    public String getProviderCode() {
        return "NAFATH";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "NAFATH_INITIATE" -> initiateResponse();
            case "NAFATH_CHECK_STATUS" -> checkStatusResponse(requestBody);
            case "NAFATH_CALLBACK" -> callbackResponse();
            case "NAFATH_GET_JWK" -> getJwkResponse();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult initiateResponse() {
        var transId = UUID.randomUUID().toString();
        var random = String.valueOf((int) (Math.random() * 90 + 10));
        // Pick the name pair ONCE per session; reuse it on every check-status poll.
        sessionNames.put(transId, newNamePair());
        var body = """
                {
                    "transId": "%s",
                    "random": "%s"
                }
                """.formatted(transId, random);
        return new MockResponseResult(201, body, HEADERS);
    }

    private MockResponseResult checkStatusResponse(String requestBody) {
        String transId = extractTransId(requestBody);
        // Reuse the name pair picked at initiate-time. If the session is unknown
        // (e.g. request replayed without prior initiate), fall back to a fresh
        // pool pick — but only one pick, no per-call rerolling within a session.
        NamePair names = transId != null
                ? sessionNames.computeIfAbsent(transId, k -> newNamePair())
                : newNamePair();
        String responseTransId = transId != null ? transId : UUID.randomUUID().toString();
        var body = """
                {
                    "aud": "https://api.awn.ai/callback/nafath",
                    "exp": 1711968180,
                    "iat": 1711968030,
                    "iss": "https://nafath.api.elm.sa",
                    "jti": "52e16851-f781-47f0-9242-4ef6916efbfa",
                    "nbf": 1711968030,
                    "sub": "2565881771",
                    "logId": 1228742529,
                    "gender": "M",
                    "status": "COMPLETED",
                    "transId": "%s",
                    "PersonId": 2564878532,
                    "jwks_uri": "https://nafath.api.elm.sa/api/v1/mfa/jwk",
                    "lastName": "نواز",
                    "firstName": "عمر",
                    "thirdName": "شيخ سياي",
                    "secondName": "شيخ",
                    "ServiceName": "OpenAccount",
                    "iqamaNumber": "2564878532",
                    "dateOfBirthG": "01-05-1993",
                    "dateOfBirthH": "10-11-1413",
                    "drivingLicenses": null,
                    "englishLastName": "",
                    "iqamaIssueDateG": "21-01-2024",
                    "iqamaIssueDateH": "09-07-1445",
                    "nationalAddress": [
                        {
                            "city": "RIYADH",
                            "cityId": "3",
                            "cityL2": "الرياض",
                            "district": "Al Woroud Dist.",
                            "postCode": "12252",
                            "regionId": "1",
                            "streetL2": "حمد الجاسر",
                            "districtID": "null",
                            "districtL2": "حي الورود",
                            "regionName": "Al Riyadh",
                            "streetName": "Hamad Al Jasir",
                            "unitNumber": "null",
                            "regionNameL2": "الرياض",
                            "shortAddress": "RHWA3482",
                            "buildingNumber": "3482",
                            "additionalNumber": "6847",
                            "isPrimaryAddress": "true",
                            "locationCoordinates": "46.68177602 24.72106887"
                        }
                    ],
                    "nationalityCode": 304,
                    "nationalityDesc": "باكستان",
                    "englishFirstName": "%s",
                    "englishThirdName": "%s",
                    "iqamaExpiryDateG": "24-03-2027",
                    "iqamaExpiryDateH": "24-09-1446",
                    "englishSecondName": "",
                    "iqamaVersionNumber": 1,
                    "iqamaIssuePlaceCode": 1,
                    "iqamaIssuePlaceDesc": "الرياض"
                }
                """.formatted(responseTransId, escape(names.first()), escape(names.third()));
        return new MockResponseResult(200, body, HEADERS);
    }

    private NamePair newNamePair() {
        return new NamePair(RecipientNamePool.randomFirstName(), RecipientNamePool.randomName());
    }

    private String extractTransId(String requestBody) {
        if (requestBody == null || requestBody.isBlank()) return null;
        Matcher m = TRANS_ID_PATTERN.matcher(requestBody);
        return m.find() ? m.group(1) : null;
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private MockResponseResult callbackResponse() {
        var body = """
                {
                    "status": "Received"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult getJwkResponse() {
        var body = """
                {
                    "keys": [
                        {
                            "kty": "RSA",
                            "kid": "mock-key-id",
                            "use": "sig",
                            "n": "mock-modulus",
                            "e": "AQAB"
                        }
                    ]
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "transId": "%s",
                    "random": "%d"
                }
                """.formatted(UUID.randomUUID().toString(), (int) (Math.random() * 90 + 10));
        return new MockResponseResult(200, body, HEADERS);
    }
}
