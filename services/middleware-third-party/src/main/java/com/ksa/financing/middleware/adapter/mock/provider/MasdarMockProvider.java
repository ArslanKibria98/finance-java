package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MasdarMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "MASDAR";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "MASDAR_AUTH" -> authResponse();
            case "MASDAR_INDIVIDUAL_PLUS" -> individualPlusResponse();
            case "MASDAR_EMPLOYMENT" -> employmentResponse();
            case "MASDAR_ADDRESS" -> addressResponse();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult authResponse() {
        var body = """
                {
                    "access_token": "mock-masdar-token-%s",
                    "token_type": "Bearer",
                    "expires_in": 3600,
                    "scope": "individual:read employment:read"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult individualPlusResponse() {
        var body = """
                {
                    "referenceNumber": "%s",
                    "statusCode": "200",
                    "statusMessage": "Success",
                    "individual": {
                        "nationalId": "1088052343",
                        "idType": "NATIONAL_ID",
                        "idExpiryDateH": "1448-09-15",
                        "idExpiryDateG": "2027-03-31",
                        "firstNameAr": "\u0641\u064a\u0635\u0644",
                        "fatherNameAr": "\u0647\u0644\u064a\u0644",
                        "grandFatherNameAr": "\u0639\u0628\u064a\u062f",
                        "familyNameAr": "\u0627\u0644\u0639\u062a\u064a\u0628\u064a",
                        "fullNameAr": "\u0641\u064a\u0635\u0644 \u0647\u0644\u064a\u0644 \u0639\u0628\u064a\u062f \u0627\u0644\u0639\u062a\u064a\u0628\u064a",
                        "firstNameEn": "FAISAL",
                        "fatherNameEn": "HULAYYIL",
                        "grandFatherNameEn": "OBAID",
                        "familyNameEn": "ALOTAIBI",
                        "fullNameEn": "FAISAL HULAYYIL OBAID ALOTAIBI",
                        "dateOfBirthH": "1420-06-19",
                        "dateOfBirthG": "1999-09-29",
                        "gender": "M",
                        "nationality": "SA",
                        "nationalityDescAr": "\u0633\u0639\u0648\u062f\u064a",
                        "maritalStatus": "1",
                        "maritalStatusDescAr": "\u0623\u0639\u0632\u0628",
                        "numberOfDependents": 0
                    },
                    "employment": {
                        "employerName": "\u0634\u0631\u0643\u0629 \u0639\u0648\u0646 \u0627\u0644\u0631\u0627\u0626\u062f\u0629 \u0644\u0644\u062a\u0645\u0648\u064a\u0644 \u0627\u0644\u0627\u0633\u062a\u0647\u0644\u0627\u0643\u064a \u0627\u0644\u0645\u0635\u063a\u0631",
                        "employerNameEn": "AWN Pioneering Micro Finance Company",
                        "sector": "PRIVATE",
                        "basicSalary": 7408.00,
                        "housingAllowance": 1851.00,
                        "otherAllowance": 0.00,
                        "totalSalary": 9259.00,
                        "joinDate": "2020-09-15",
                        "workingMonths": 54,
                        "status": "ACTIVE",
                        "statusAr": "\u0646\u0634\u064a\u0637",
                        "gosiSubscription": true
                    },
                    "address": {
                        "buildingNumber": "4302",
                        "streetName": "No. 338",
                        "district": "Al Narjis Dist.",
                        "city": "Riyadh",
                        "region": "Al Riyadh",
                        "postCode": "13327",
                        "additionalNumber": "6847",
                        "country": "SA"
                    }
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult employmentResponse() {
        var body = """
                {
                    "referenceNumber": "%s",
                    "statusCode": "200",
                    "statusMessage": "Success",
                    "employment": {
                        "employerName": "\u0634\u0631\u0643\u0629 \u0639\u0648\u0646 \u0627\u0644\u0631\u0627\u0626\u062f\u0629 \u0644\u0644\u062a\u0645\u0648\u064a\u0644 \u0627\u0644\u0627\u0633\u062a\u0647\u0644\u0627\u0643\u064a \u0627\u0644\u0645\u0635\u063a\u0631",
                        "employerNameEn": "AWN Pioneering Micro Finance Company",
                        "crNumber": "7025558920",
                        "sector": "PRIVATE",
                        "basicSalary": 7408.00,
                        "housingAllowance": 1851.00,
                        "otherAllowance": 0.00,
                        "totalSalary": 9259.00,
                        "joinDate": "2020-09-15",
                        "workingMonths": 54,
                        "status": "ACTIVE",
                        "gosiSubscription": true,
                        "socialInsuranceNumber": "123456789"
                    }
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult addressResponse() {
        var body = """
                {
                    "referenceNumber": "%s",
                    "statusCode": "200",
                    "statusMessage": "Success",
                    "address": {
                        "isPrimary": true,
                        "buildingNumber": "4302",
                        "streetNameAr": "\u0634\u0627\u0631\u0639 338",
                        "streetNameEn": "No. 338",
                        "districtAr": "\u062d\u064a \u0627\u0644\u0646\u0631\u062c\u0633",
                        "districtEn": "Al Narjis Dist.",
                        "cityAr": "\u0627\u0644\u0631\u064a\u0627\u0636",
                        "cityEn": "Riyadh",
                        "regionAr": "\u0645\u0646\u0637\u0642\u0629 \u0627\u0644\u0631\u064a\u0627\u0636",
                        "regionEn": "Al Riyadh",
                        "postCode": "13327",
                        "additionalNumber": "6847",
                        "country": "SA",
                        "latitude": 24.72106887,
                        "longitude": 46.68177602
                    }
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "referenceNumber": "%s",
                    "statusCode": "200",
                    "statusMessage": "Success"
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }
}
