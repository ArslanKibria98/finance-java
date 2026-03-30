package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SafeWatchMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "SAFEWATCH";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "SAFEWATCH_SCAN_SESSION" -> scanSession();
            case "SAFEWATCH_SCAN_DETAILS" -> scanSession();
            case "SAFEWATCH_ADD_CUSTOMER" -> addCustomer();
            case "SAFEWATCH_ADD_CUSTOMER_ADDRESS" -> addCustomerAddress();
            case "SAFEWATCH_ADD_CUSTOMER_IDENTITY" -> addCustomerIdentity();
            case "SAFEWATCH_ADD_INDIVIDUAL_CUSTOMER" -> addIndividualCustomer();
            case "SAFEWATCH_ADD_ACCOUNT" -> addAccount();
            case "SAFEWATCH_ADD_CUSTOMER_ACCOUNT" -> addCustomerAndAccount();
            case "SAFEWATCH_ADD_TRANSACTION_OFFLINE" -> addTransactionOffline();
            case "SAFEWATCH_ADD_PHYSICAL_CUSTOMER_DECLARATION" -> addPhysicalCustomerDeclaration();
            case "SAFEWATCH_ADD_ACCOUNT_EXTENDED_DECLARATION" -> addAccountExtendedDeclaration();
            default -> fallbackResponse();
        };
    }

    private MockResponseResult scanSession() {
        var body = """
                {
                    "clientRequestId": 117916,
                    "elapsedTimeMillis": 554,
                    "text": "0 violation(s) found",
                    "violationCounter": 0
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult addCustomer() {
        var clientKey = UUID.randomUUID().toString();
        var body = """
                {
                    "return": "SUCCESS",
                    "message": "SUCCESS / -DataIds already exists / -Customer already exists / -CustomerName already exists",
                    "response": {
                        "id": 64,
                        "clientKey": "%s",
                        "name": {
                            "firstName": "KHALID",
                            "middleName": "SULAIMAN",
                            "lastName": "M",
                            "alternativeName": null,
                            "latinFirstName": null,
                            "latinLastName": null,
                            "latinMiddleName": null,
                            "latinPrefixName": null,
                            "manualLoaded": "N",
                            "zoneId": "1",
                            "languageName": null,
                            "languageId": null
                        },
                        "typeId": "1",
                        "typeName": "01",
                        "subTypeId": null,
                        "subTypeName": null,
                        "active": "N",
                        "statusId": "1",
                        "statusName": "01",
                        "branchId": "1",
                        "branchName": "37040044",
                        "monitored": "Y",
                        "fatca": "N",
                        "riskLevelId": null,
                        "riskLevelName": null,
                        "complianceRiskLevel": 0,
                        "detectionScore": 0,
                        "registration": "N",
                        "residency": "SA",
                        "nationale": "N",
                        "tradeFinance": "N",
                        "issuedShares": null,
                        "ownershipPercentage": null,
                        "totalNumberOfShares": "",
                        "zoneId": 1,
                        "addresses": [
                            {
                                "id": 28,
                                "clientKey": "%s",
                                "addressTypeId": "1",
                                "addressTypeName": "R",
                                "streetName": "No. 338",
                                "streetNumber": "4302",
                                "district": "Al Narjis Dist.",
                                "postcode": "13327",
                                "city": "Riyadh",
                                "county": "Saudi Arabia",
                                "country": "SA",
                                "currentAddr": "Y",
                                "phone": "966501088642",
                                "phoneWork": "966501088642",
                                "mobile": "966501088642",
                                "fax": "",
                                "email": null,
                                "pobox": null,
                                "addressAdd1": "",
                                "addressAdd2": "",
                                "zoneId": 1
                            }
                        ],
                        "identity": [
                            {
                                "id": 30,
                                "clientKey": "%s",
                                "identificationTypeId": "1",
                                "identificationTypeName": "02",
                                "idNumber": "1088052343",
                                "issuer": "Saudi Arabia",
                                "issuerCountry": "SA",
                                "issuerDate": "20220525",
                                "expiryDate": "20270331",
                                "manualLoaded": "N",
                                "zoneId": "1"
                            }
                        ],
                        "physical": {
                            "id": 25,
                            "clientKey": "%s",
                            "gender": "M",
                            "maritalStateId": null,
                            "maritalStateName": null,
                            "nbChildren": 0,
                            "fatherName": "\u0633\u0644\u064a\u0645\u0627\u0646",
                            "motherName": "",
                            "spouseName": "",
                            "dateOfBirth": "19950926",
                            "cityOfBirth": "Riyadh",
                            "countryOfBirth": "SA",
                            "pep": "N",
                            "vip": "N",
                            "employee": "N",
                            "nationality": "SA",
                            "otherNationality": null,
                            "sectorId": null,
                            "sectorName": null,
                            "occupationId": "1",
                            "occupationName": "01",
                            "secondOccupationId": null,
                            "secondOccupationName": null,
                            "occupationDesc": null,
                            "socialSecurity": null,
                            "taxIdentification": null,
                            "zoneId": 1
                        },
                        "entity": null,
                        "financial": null,
                        "audit": []
                    }
                }
                """.formatted(clientKey, clientKey, clientKey, clientKey);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult addCustomerAddress() {
        var clientKey = UUID.randomUUID().toString();
        var body = """
                {
                    "return": "SUCCESS",
                    "message": "SUCCESS",
                    "response": {
                        "id": 29,
                        "clientKey": "%s",
                        "addressTypeId": "1",
                        "addressTypeName": "R",
                        "streetName": "Muhammad Ibn Al Qoba",
                        "streetNumber": "4063",
                        "district": "Al Khaleej Dist.",
                        "postcode": "13224",
                        "city": "Riyadh",
                        "county": "Saudi Arabia",
                        "country": "SA",
                        "currentAddr": "Y",
                        "phone": "966590933288",
                        "phoneWork": "966590933288",
                        "mobile": "966590933288",
                        "fax": "",
                        "email": null,
                        "pobox": null,
                        "addressAdd1": "",
                        "addressAdd2": "",
                        "zoneId": 1
                    }
                }
                """.formatted(clientKey);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult addCustomerIdentity() {
        var clientKey = UUID.randomUUID().toString();
        var body = """
                {
                    "return": "SUCCESS",
                    "message": "SUCCESS",
                    "response": {
                        "id": 31,
                        "clientKey": "%s",
                        "identificationTypeId": "1",
                        "identificationTypeName": "02",
                        "idNumber": "1108149475",
                        "issuer": "Saudi Arabia",
                        "issuerCountry": "SA",
                        "issuerDate": "20240124",
                        "expiryDate": "20281128",
                        "manualLoaded": "N",
                        "zoneId": "1"
                    }
                }
                """.formatted(clientKey);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult addIndividualCustomer() {
        var clientKey = UUID.randomUUID().toString();
        var body = """
                {
                    "return": "SUCCESS",
                    "message": "SUCCESS",
                    "response": {
                        "id": 25,
                        "clientKey": "%s",
                        "gender": "M",
                        "maritalStateId": null,
                        "maritalStateName": null,
                        "nbChildren": 0,
                        "fatherName": "\u0647\u0644\u064a\u0644",
                        "motherName": "",
                        "spouseName": "",
                        "dateOfBirth": "19990929",
                        "cityOfBirth": "Riyadh",
                        "countryOfBirth": "SA",
                        "pep": "N",
                        "vip": "N",
                        "employee": "N",
                        "nationality": "SA",
                        "otherNationality": null,
                        "sectorId": null,
                        "sectorName": null,
                        "occupationId": "1",
                        "occupationName": "01",
                        "zoneId": 1
                    }
                }
                """.formatted(clientKey);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult addAccount() {
        var clientKey = UUID.randomUUID().toString();
        var body = """
                {
                    "return": "SUCCESS",
                    "message": "-DataIds already exists / -Account already exists / -AccountName already exists",
                    "response": {
                        "id": 46,
                        "clientKey": "%s",
                        "accName": null,
                        "accNumber": "%s",
                        "accIban": "******************4701",
                        "statusId": "1",
                        "statusName": "01",
                        "typeId": "1",
                        "typeName": "SAV",
                        "subTypeId": null,
                        "subTypeName": null,
                        "channelId": "1",
                        "channelName": "1",
                        "branchId": "1",
                        "branchName": "37040044",
                        "monitored": "N",
                        "openDate": "20250626",
                        "closeDate": null,
                        "currency": "SAR",
                        "complianceRiskLevel": 0,
                        "zoneId": 1,
                        "riskLevelName": null,
                        "riskLevelId": null
                    }
                }
                """.formatted(clientKey, UUID.randomUUID().toString().substring(0, 30));
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult addCustomerAndAccount() {
        var clientKey = UUID.randomUUID().toString();
        var body = """
                {
                    "return": "SUCCESS",
                    "message": "SUCCESS / -DataIds already exists / -Connection already exists",
                    "response": {
                        "id": 167,
                        "fromCustomerClientKey": "%s",
                        "toAccountClientKey": "%s",
                        "toCustomerClientKey": null,
                        "toEntityType": "3",
                        "connectionType": "1",
                        "startDate": "20250706",
                        "endDate": null,
                        "deleted": 0,
                        "manual": 0,
                        "zoneId": 1
                    }
                }
                """.formatted(clientKey, clientKey);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult addTransactionOffline() {
        var body = """
                {
                    "return": "SUCCESS",
                    "message": ""
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult addPhysicalCustomerDeclaration() {
        var clientKey = UUID.randomUUID().toString();
        var body = """
                {
                    "return": "SUCCESS",
                    "message": "Declaration processed successfully for customer %s"
                }
                """.formatted(clientKey);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult addAccountExtendedDeclaration() {
        var clientKey = UUID.randomUUID().toString();
        var body = """
                {
                    "return": "SUCCESS",
                    "message": "Extended Declaration processed successfully for account %s"
                }
                """.formatted(clientKey);
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "return": "SUCCESS",
                    "message": "SAFEWATCH request processed"
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }
}
