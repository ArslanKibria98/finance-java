package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class WathqMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "WATHQ";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "WATHQ_VERIFY_CR" -> verifyCr();
            case "WATHQ_GET_COMPANY_INFO" -> getCompanyInfo();
            case "WATHQ_GET_FREELANCE" -> getFreelanceLicense();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult verifyCr() {
        var body = """
                {
                    "referenceNumber": "%s",
                    "statusCode": "200",
                    "statusMessage": "Success",
                    "crInfo": {
                        "crNumber": "7025558920",
                        "companyNameAr": "\u0634\u0631\u0643\u0629 \u0639\u0648\u0646 \u0627\u0644\u0631\u0627\u0626\u062f\u0629 \u0644\u0644\u062a\u0645\u0648\u064a\u0644 \u0627\u0644\u0627\u0633\u062a\u0647\u0644\u0627\u0643\u064a \u0627\u0644\u0645\u0635\u063a\u0631",
                        "companyNameEn": "AWN Pioneering Micro Finance Company",
                        "status": "ACTIVE",
                        "statusAr": "\u0642\u0627\u0626\u0645",
                        "issueDateH": "1444-06-10",
                        "issueDateG": "2023-01-03",
                        "expiryDateH": "1448-06-10",
                        "expiryDateG": "2027-01-01",
                        "businessType": "LIMITED_LIABILITY",
                        "businessTypeAr": "\u0634\u0631\u0643\u0629 \u0630\u0627\u062a \u0645\u0633\u0624\u0648\u0644\u064a\u0629 \u0645\u062d\u062f\u0648\u062f\u0629",
                        "cityAr": "\u0627\u0644\u0631\u064a\u0627\u0636",
                        "cityEn": "Riyadh",
                        "capital": 500000000.00,
                        "currency": "SAR",
                        "fiscalYearEndDate": "12-31",
                        "moiNumber": "1010987654",
                        "verified": true
                    }
                }
                """.formatted(UUID.randomUUID().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult getCompanyInfo() {
        var body = """
                {
                    "referenceNumber": "%s",
                    "statusCode": "200",
                    "statusMessage": "Success",
                    "crInfo": {
                        "crNumber": "7025558920",
                        "companyNameAr": "\u0634\u0631\u0643\u0629 \u0639\u0648\u0646 \u0627\u0644\u0631\u0627\u0626\u062f\u0629 \u0644\u0644\u062a\u0645\u0648\u064a\u0644 \u0627\u0644\u0627\u0633\u062a\u0647\u0644\u0627\u0643\u064a \u0627\u0644\u0645\u0635\u063a\u0631",
                        "companyNameEn": "AWN Pioneering Micro Finance Company",
                        "status": "ACTIVE",
                        "businessType": "LIMITED_LIABILITY",
                        "capital": 500000000.00,
                        "currency": "SAR"
                    },
                    "activities": [
                        {
                            "code": "6492",
                            "descriptionAr": "\u0623\u0646\u0634\u0637\u0629 \u0627\u0644\u062a\u0645\u0648\u064a\u0644 \u0627\u0644\u0623\u062e\u0631\u0649",
                            "descriptionEn": "Other credit granting",
                            "isPrimary": true
                        },
                        {
                            "code": "6499",
                            "descriptionAr": "\u0623\u0646\u0634\u0637\u0629 \u062e\u062f\u0645\u0627\u062a \u0645\u0627\u0644\u064a\u0629 \u0623\u062e\u0631\u0649",
                            "descriptionEn": "Other financial service activities",
                            "isPrimary": false
                        }
                    ],
                    "shareholders": [
                        {
                            "name": "\u0634\u0631\u0643\u0629 \u0639\u0648\u0646 \u0627\u0644\u0642\u0627\u0628\u0636\u0629",
                            "nameEn": "AWN Holding Company",
                            "share": 100.0,
                            "type": "CORPORATE"
                        }
                    ],
                    "management": [
                        {
                            "name": "\u0645\u062d\u0645\u062f \u0627\u0644\u0639\u062a\u064a\u0628\u064a",
                            "nameEn": "Mohammed Alotaibi",
                            "role": "GENERAL_MANAGER",
                            "roleAr": "\u0645\u062f\u064a\u0631 \u0639\u0627\u0645"
                        }
                    ],
                    "retrievedAt": "%s"
                }
                """.formatted(UUID.randomUUID().toString(), Instant.now().toString());
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult getFreelanceLicense() {
        var body = """
                {
                    "referenceNumber": "%s",
                    "statusCode": "200",
                    "statusMessage": "Success",
                    "freelanceInfo": {
                        "licenseNumber": "FL-123456",
                        "holderNameAr": "\u0641\u064a\u0635\u0644 \u0627\u0644\u0639\u062a\u064a\u0628\u064a",
                        "holderNameEn": "FAISAL ALOTAIBI",
                        "nationalId": "1088052343",
                        "status": "ACTIVE",
                        "issueDateG": "2024-01-15",
                        "expiryDateG": "2025-01-14",
                        "activityCode": "6201",
                        "activityDescAr": "\u0628\u0631\u0645\u062c\u0629 \u0627\u0644\u062d\u0627\u0633\u0628",
                        "activityDescEn": "Computer Programming"
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
