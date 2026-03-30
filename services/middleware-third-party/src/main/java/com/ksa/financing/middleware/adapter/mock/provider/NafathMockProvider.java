package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NafathMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "NAFATH";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "NAFATH_INITIATE" -> initiateResponse();
            case "NAFATH_CHECK_STATUS" -> checkStatusResponse();
            case "NAFATH_CALLBACK" -> callbackResponse();
            case "NAFATH_GET_JWK" -> getJwkResponse();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult initiateResponse() {
        var transId = UUID.randomUUID().toString();
        var random = String.valueOf((int) (Math.random() * 90 + 10));
        var body = """
                {
                    "transId": "%s",
                    "random": "%s"
                }
                """.formatted(transId, random);
        return new MockResponseResult(201, body, HEADERS);
    }

    private MockResponseResult checkStatusResponse() {
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
                    "lastName": "\u0646\u0648\u0627\u0632",
                    "firstName": "\u0639\u0645\u0631",
                    "thirdName": "\u0634\u064a\u062e \u0633\u064a\u0627\u064a",
                    "secondName": "\u0634\u064a\u062e",
                    "ServiceName": "OpenAccount",
                    "iqamaNumber": "2564878532",
                    "dateOfBirthG": "01-05-1993",
                    "dateOfBirthH": "10-11-1413",
                    "drivingLicenses": null,
                    "englishLastName": "NAWAZ",
                    "iqamaIssueDateG": "21-01-2024",
                    "iqamaIssueDateH": "09-07-1445",
                    "nationalAddress": [
                        {
                            "city": "RIYADH",
                            "cityId": "3",
                            "cityL2": "\u0627\u0644\u0631\u064a\u0627\u0636",
                            "district": "Al Woroud Dist.",
                            "postCode": "12252",
                            "regionId": "1",
                            "streetL2": "\u062d\u0645\u062f \u0627\u0644\u062c\u0627\u0633\u0631",
                            "districtID": "null",
                            "districtL2": "\u062d\u064a \u0627\u0644\u0648\u0631\u0648\u062f",
                            "regionName": "Al Riyadh",
                            "streetName": "Hamad Al Jasir",
                            "unitNumber": "null",
                            "regionNameL2": "\u0627\u0644\u0631\u064a\u0627\u0636",
                            "shortAddress": "RHWA3482",
                            "buildingNumber": "3482",
                            "additionalNumber": "6847",
                            "isPrimaryAddress": "true",
                            "locationCoordinates": "46.68177602 24.72106887"
                        }
                    ],
                    "nationalityCode": 304,
                    "nationalityDesc": "\u0628\u0627\u0643\u0633\u062a\u0627\u0646",
                    "englishFirstName": "OMAR",
                    "englishThirdName": "SHEIKH",
                    "iqamaExpiryDateG": "24-03-2027",
                    "iqamaExpiryDateH": "24-09-1446",
                    "englishSecondName": "NAWAZ",
                    "iqamaVersionNumber": 1,
                    "iqamaIssuePlaceCode": 1,
                    "iqamaIssuePlaceDesc": "\u0627\u0644\u0631\u064a\u0627\u0636"
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
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
