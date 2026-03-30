package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AbsherMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "ABSHER";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "ABSHER_VERIFY_IDENTITY" -> verifyIdentityResponse();
            case "ABSHER_GET_ADDRESS" -> getAddressResponse();
            case "ABSHER_GET_DEPENDENTS" -> getDependentsResponse();
            case "ABSHER_VERIFY_IQAMA" -> verifyIqamaResponse();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult verifyIdentityResponse() {
        var body = """
                {
                    "referenceNumber": "%s",
                    "status": "VERIFIED",
                    "personInfo": {
                        "idNumber": "1088052343",
                        "idType": "NATIONAL_ID",
                        "idVersion": 2,
                        "idExpiryDateH": "1448-09-15",
                        "idExpiryDateG": "2027-03-31",
                        "idIssuePlaceAr": "\u0627\u0644\u0631\u064a\u0627\u0636",
                        "idIssuePlaceEn": "Riyadh",
                        "firstNameAr": "\u062e\u0627\u0644\u062f",
                        "fatherNameAr": "\u0633\u0644\u064a\u0645\u0627\u0646",
                        "grandFatherNameAr": "\u0645\u062d\u0645\u062f",
                        "familyNameAr": "\u0627\u0644\u0639\u062a\u064a\u0628\u064a",
                        "firstNameEn": "KHALID",
                        "fatherNameEn": "SULAIMAN",
                        "grandFatherNameEn": "MOHAMMED",
                        "familyNameEn": "ALOTAIBI",
                        "gender": "M",
                        "dateOfBirthH": "1416-02-03",
                        "dateOfBirthG": "1995-09-26",
                        "nationality": "SA",
                        "nationalityDescAr": "\u0633\u0639\u0648\u062f\u064a",
                        "nationalityDescEn": "Saudi",
                        "maritalStatus": "S",
                        "maritalStatusDescAr": "\u0623\u0639\u0632\u0628",
                        "occupationCode": "0001",
                        "occupationDescAr": "\u0645\u0648\u0638\u0641 \u0642\u0637\u0627\u0639 \u062e\u0627\u0635"
                    }
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult getAddressResponse() {
        var body = """
                {
                    "referenceNumber": "%s",
                    "addressInfo": {
                        "isPrimaryAddress": true,
                        "buildingNumber": "4302",
                        "streetNameAr": "\u0634\u0627\u0631\u0639 \u0627\u0644\u0623\u0645\u064a\u0631 \u0645\u062d\u0645\u062f",
                        "streetNameEn": "Prince Mohammed Street",
                        "districtAr": "\u062d\u064a \u0627\u0644\u0646\u0631\u062c\u0633",
                        "districtEn": "Al Narjis Dist.",
                        "cityAr": "\u0627\u0644\u0631\u064a\u0627\u0636",
                        "cityEn": "Riyadh",
                        "regionAr": "\u0645\u0646\u0637\u0642\u0629 \u0627\u0644\u0631\u064a\u0627\u0636",
                        "regionEn": "Al Riyadh",
                        "postCode": "13327",
                        "additionalNumber": "6847",
                        "unitNumber": null,
                        "shortAddress": "RINA4302",
                        "locationCoordinates": {
                            "latitude": 24.72106887,
                            "longitude": 46.68177602
                        }
                    }
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult getDependentsResponse() {
        var body = """
                {
                    "referenceNumber": "%s",
                    "dependentsCount": 0,
                    "dependents": []
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult verifyIqamaResponse() {
        var body = """
                {
                    "referenceNumber": "%s",
                    "status": "VERIFIED",
                    "iqamaInfo": {
                        "iqamaNumber": "2564878532",
                        "idExpiryDateH": "1448-09-24",
                        "idExpiryDateG": "2027-03-24",
                        "idIssueDateH": "1445-07-09",
                        "idIssueDateG": "2024-01-21",
                        "idIssuePlaceAr": "\u0627\u0644\u0631\u064a\u0627\u0636",
                        "firstNameAr": "\u0639\u0645\u0631",
                        "fatherNameAr": "\u0634\u064a\u062e",
                        "grandFatherNameAr": "\u0633\u064a\u0627\u064a",
                        "familyNameAr": "\u0646\u0648\u0627\u0632",
                        "firstNameEn": "OMAR",
                        "fatherNameEn": "NAWAZ",
                        "grandFatherNameEn": "SHEIKH",
                        "familyNameEn": "NAWAZ",
                        "gender": "M",
                        "dateOfBirthG": "1993-05-01",
                        "nationality": "PK",
                        "nationalityDescAr": "\u0628\u0627\u0643\u0633\u062a\u0627\u0646\u064a",
                        "nationalityCode": 304,
                        "occupationCode": "0001",
                        "sponsorId": "7025558920",
                        "sponsorName": "\u0634\u0631\u0643\u0629 \u0639\u0648\u0646 \u0627\u0644\u0631\u0627\u0626\u062f\u0629 \u0644\u0644\u062a\u0645\u0648\u064a\u0644 \u0627\u0644\u0627\u0633\u062a\u0647\u0644\u0627\u0643\u064a \u0627\u0644\u0645\u0635\u063a\u0631",
                        "iqamaVersionNumber": 1
                    }
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "referenceNumber": "%s",
                    "status": "SUCCESS",
                    "message": "ABSHER request processed"
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }
}
