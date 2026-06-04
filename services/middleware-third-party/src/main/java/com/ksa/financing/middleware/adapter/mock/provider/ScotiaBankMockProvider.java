package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ScotiaBankMockProvider implements MockResponseProvider {

    private static final String HEADERS = "Content-Type: application/json";

    @Override
    public String getProviderCode() {
        return "SCOTIABANK";
    }

    @Override
    public MockResponseResult getMockResponse(String apiCode, String requestBody) {
        return switch (apiCode) {
            case "SCOTIABANK_PAYMENT_VALIDATE" -> validatePaymentResponse();
            case "SCOTIABANK_PAYMENT_CREATE" -> createPaymentResponse();
            case "SCOTIABANK_PAYMENT_OPTIONS_INQUIRY" -> paymentOptionsInquiryResponse();
            case "SCOTIABANK_PAYMENT_COMMIT" -> commitTransactionResponse();
            case "SCOTIABANK_PAYMENT_CANCEL" -> cancelPaymentResponse();
            case "SCOTIABANK_PAYMENT_SUMMARY" -> paymentSummaryResponse();
            case "SCOTIABANK_PAYMENT_DETAILS" -> paymentDetailsResponse();
            // Wire rail (one-time wire transfer)
            case "SCOTIABANK_WIRE_VALIDATE" -> validatePaymentResponse();
            case "SCOTIABANK_WIRE_CREATE" -> createPaymentResponse();
            case "SCOTIABANK_WIRE_INQUIRE" -> wireInquireResponse();
            // EFT rail (local Canada fund transfer)
            case "SCOTIABANK_ACCOUNT_VALIDATION" -> accountValidationResponse();
            case "SCOTIABANK_EFT_CREATE" -> eftCreateResponse();
            case "SCOTIABANK_EFT_SUBMIT" -> eftSubmitResponse();
            case "SCOTIABANK_EFT_INQUIRE" -> eftInquireResponse();
            default -> fallbackResponse();
        };
    }

    // ===================== EFT RAIL =====================

    // POST /treasury/validation/v2/account-validation
    private MockResponseResult accountValidationResponse() {
        var body = """
                {
                    "data": {
                        "validation_status": [
                            [
                                {
                                    "type": "format",
                                    "status": "valid",
                                    "addendum": {
                                        "supported_transaction_types": ["cr", "dr"]
                                    }
                                },
                                {
                                    "type": "account_status",
                                    "status": "active"
                                },
                                {
                                    "type": "name_match",
                                    "status": "high"
                                },
                                {
                                    "type": "transaction_activity",
                                    "status": "high"
                                }
                            ]
                        ]
                    },
                    "notifications": []
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    // POST /treasury/payments/eft/v1/payments — creates an EFT submission (draft)
    private MockResponseResult eftCreateResponse() {
        var body = """
                {
                    "data": {
                        "submission_id": "1000000001",
                        "submission_status": "CREATED",
                        "submission_creation_date_time": "2023-04-19 21:17:56.946",
                        "agreement_id": "SD123456789",
                        "number_of_transactions": "2",
                        "accepted_transactions_count": "2",
                        "rejected_transactions_count": "0",
                        "accepted_transactions": [
                            {
                                "message_identification": "1000000001",
                                "end_to_end_identification": "2000000001",
                                "payment_id": "2000000001",
                                "payment_status": "CREATED",
                                "payment_creation_date_time": "2023-04-19T17:18:01.143Z"
                            },
                            {
                                "message_identification": "1000000001",
                                "end_to_end_identification": "2000000002",
                                "payment_id": "2000000002",
                                "payment_status": "CREATED",
                                "payment_creation_date_time": "2023-04-19T17:18:01.186Z"
                            }
                        ]
                    },
                    "notifications": []
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    // POST /treasury/payments/eft/v1/submissions/{submissionId} — submit for processing
    private MockResponseResult eftSubmitResponse() {
        var body = """
                {
                    "data": {
                        "submission_id": "1000000001",
                        "submission_status": "CREATED",
                        "submission_creation_date_time": "2022-10-01T10:20:26.564Z"
                    },
                    "notifications": []
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    // GET /treasury/payments/eft/v1/submissions/{submissionId} — status inquiry
    private MockResponseResult eftInquireResponse() {
        var body = """
                {
                    "data": {
                        "submission_id": "1000000001",
                        "status": "COMPLETED",
                        "number_of_transactions": "2",
                        "payments": [
                            {
                                "end_to_end_identification": "2000000001",
                                "credit_debit_indicator": "CRDT",
                                "status": "SETTLED"
                            },
                            {
                                "end_to_end_identification": "2000000002",
                                "credit_debit_indicator": "DBIT",
                                "status": "SETTLED"
                            }
                        ]
                    },
                    "notifications": []
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    // POST /treasury/payments/wire/v1/payments/validate (SCOTIABANK_WIRE_VALIDATE)
    private MockResponseResult validatePaymentResponse() {
        var body = """
                {
                    "data": {
                        "requested_execution_date": "2023-07-17",
                        "value_date": "2023-07-17"
                    },
                    "notifications": []
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    // POST /treasury/payments/wire/v1/payments (SCOTIABANK_WIRE_CREATE)
    private MockResponseResult createPaymentResponse() {
        var body = """
                {
                    "data": {
                        "payment_id": "17000000001",
                        "requested_execution_date": "2023-07-17",
                        "value_date": "2023-07-17",
                        "status": "CREATED"
                    },
                    "notifications": []
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    // GET /treasury/payments/wire/v1/payments/{payment-id} (SCOTIABANK_WIRE_INQUIRE)
    private MockResponseResult wireInquireResponse() {
        var body = """
                {
                    "data": {
                        "payment_id": "17000000001",
                        "requested_execution_date": "2023-07-17",
                        "value_date": "2023-07-17",
                        "status": "CREATED",
                        "message_identification": "d9ff0bb9-5d6a-4b76-a2ac-29ac4378a11b",
                        "initiation": {
                            "payment_identification": {
                                "instruction_identification": "707550001671",
                                "end_to_end_identification": "707550001671"
                            },
                            "payment_type_information": {
                                "instruction_priority": "NORM",
                                "service_level": [
                                    { "code": "BKTR" },
                                    { "proprietary": "proprietary" }
                                ],
                                "local_instrument": { "code": "DDMC" }
                            },
                            "amount": {
                                "instructed_amount": { "amount": 500, "currency": "CAD" },
                                "equivalent_amount": {
                                    "amount": { "amount": 500, "currency": "CAD" },
                                    "currency_of_transfer": "CAD"
                                }
                            },
                            "exchange_rate_information": {},
                            "charge_bearer": "CRED",
                            "debtor": {
                                "name": "TranXact Wire Profile IST",
                                "postal_address": {
                                    "department": "XYZ Department",
                                    "sub_department": "XYZ Sub Department",
                                    "street_name": "XYZ Street Name",
                                    "building_number": "1000",
                                    "building_name": "XYZ Building",
                                    "floor": "1st",
                                    "post_box": "12345",
                                    "room": "101",
                                    "post_code": "M1N 2N0",
                                    "town_name": "Scarborough",
                                    "town_location_name": "XYZ Location",
                                    "district_name": "XYZ Address",
                                    "country_sub_division": "CA",
                                    "country": "CA",
                                    "address_line": [ "2221", "Eglinton Ave E" ]
                                },
                                "identification": {
                                    "organisation_identification": { "any_bic": null, "lei": null, "other": [] }
                                },
                                "country_of_residence": "CA"
                            },
                            "debtor_account": {
                                "identification": {
                                    "other": { "identification": "002-80002-0000515", "scheme_name": { "code": "BBAN" } }
                                },
                                "type": { "proprietary": "DDA" },
                                "currency": "CAD",
                                "name": null,
                                "proxy": { "type": null, "identification": null }
                            },
                            "creditor": {
                                "name": "ABC Technologies",
                                "postal_address": {
                                    "department": "ABC Department",
                                    "sub_department": "ABC Sub Department",
                                    "street_name": "ABC Street Name",
                                    "building_number": "2000",
                                    "building_name": "ABC Building",
                                    "floor": "1st",
                                    "post_box": "12345",
                                    "room": "101",
                                    "post_code": "M1T 3S5",
                                    "town_name": "ABC Town",
                                    "town_location_name": "ABC Location",
                                    "district_name": "ABC District",
                                    "country_sub_division": "TG",
                                    "country": "IN",
                                    "address_line": [ "ABC Address Line 1", "ABC Address Line 2", "ABC Address Line 3" ]
                                },
                                "identification": {
                                    "organisation_identification": { "any_bic": null, "lei": null, "other": [] }
                                },
                                "country_of_residence": "CA"
                            },
                            "creditor_agent": {
                                "financial_institution_identification": {
                                    "bicfi": "ACCTCA61XXX",
                                    "clearing_system_member_identification": {
                                        "clearing_system_identification": { "code": "CACPA" },
                                        "member_identification": "asdf"
                                    },
                                    "lei": "FEL54W8GMYI4IYZ2NR37",
                                    "name": "string",
                                    "postal_address": {
                                        "department": "department",
                                        "sub_department": "subdepartment",
                                        "street_name": "streetname",
                                        "building_number": "200",
                                        "building_name": "buildingname",
                                        "floor": "2",
                                        "post_box": "464464",
                                        "room": "200",
                                        "post_code": "M7U 5D9",
                                        "town_name": "townname",
                                        "town_location_name": "townlocationname",
                                        "district_name": "districtname",
                                        "country_sub_division": "countrysubdivision",
                                        "country": "CA",
                                        "address_line": [ "address_line_1" ]
                                    }
                                }
                            },
                            "creditor_account": {
                                "identification": {
                                    "other": { "identification": "353453535", "scheme_name": { "code": "BBAN" } }
                                },
                                "type": { "code": "CACC" },
                                "currency": "CAD",
                                "name": null,
                                "proxy": { "type": null, "identification": null }
                            },
                            "purpose": {},
                            "remittance_information": { "structured": [] }
                        }
                    },
                    "notifications": null
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    // POST /v1/payment-options/inquiry
    private MockResponseResult paymentOptionsInquiryResponse() {
        var body = """
                {
                    "data": {
                        "payment_options": [
                            {
                                "payment_type": "REALTIME_ACCOUNT_DEPOSIT_PAYMENT",
                                "sender_account_identifier_required": false,
                                "max_payment_outgoing_amount": {
                                    "amount": 25000,
                                    "currency": "CAD"
                                }
                            }
                        ]
                    },
                    "notifications": []
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    // POST /v1/payments/secure/commit-transaction
    private MockResponseResult commitTransactionResponse() {
        var body = """
                {
                    "data": {
                        "payment_id": "6000792002",
                        "clearing_system_reference": "Z2Bk4dJdjcH8",
                        "status": "SUCCESS"
                    },
                    "notifications": []
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    // POST /v1/payments/{payment-id}/cancel
    private MockResponseResult cancelPaymentResponse() {
        var body = """
                {
                    "data": {
                        "status": "SUCCESS"
                    },
                    "notifications": []
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    // GET /v1/payments/{id}/summary
    private MockResponseResult paymentSummaryResponse() {
        var body = """
                {
                    "data": {
                        "clearing_system_reference": "C1DyRvKua6bu",
                        "product_code": "DOMESTIC",
                        "payment_type": "REALTIME_ACCOUNT_DEPOSIT_PAYMENT",
                        "request_date": "2022-05-17T13:40:02Z",
                        "amount": {
                            "amount": 2,
                            "currency": "CAD"
                        },
                        "receiving_channel_indicator": "ETRANSFER_SYSTEM",
                        "payment_status": "REALTIME_DEPOSIT_COMPLETED",
                        "expiry_date": "2022-06-16T13:39:55Z",
                        "payment_id": "6000792097",
                        "originating_channel_indicator": "ONLINE",
                        "authentication_type": "NOT_REQUIRED",
                        "customer_account": {
                            "account_holder_name": "Test Debtor Corp Inc.",
                            "bank_account_identifier": {
                                "type": "CANADIAN",
                                "account": "002-80150-0000000"
                            }
                        },
                        "additional_remittance_info": "ADVICE_DETAILS"
                    },
                    "notifications": []
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    // GET /treasury/payments/rtp/v1/payments/{payment-id} — FI-to-FI payment status report
    private MockResponseResult paymentDetailsResponse() {
        var body = """
                {
                    "data": {
                        "fi_to_fi_payment_status_report": {
                            "group_header": {
                                "message_identification": "1652795635928phhd",
                                "creation_datetime": "2022-05-17T13:53:55.928Z",
                                "instructing_agent": {
                                    "financial_institution_identification": {
                                        "clearing_system_member_identification": {
                                            "member_identification": "CA000002"
                                        }
                                    }
                                },
                                "instructed_agent": {
                                    "financial_institution_identification": {
                                        "clearing_system_member_identification": {
                                            "member_identification": "CA000002"
                                        }
                                    }
                                }
                            },
                            "transaction_information_and_status": [
                                {
                                    "original_group_information": {
                                        "original_message_identification": "1652795634715",
                                        "original_message_name_identification": "pacs.028.001.02"
                                    },
                                    "original_end_to_end_identification": "NOTPROVIDED",
                                    "original_transaction_identification": "2088000000030203007",
                                    "transaction_status": "ACSP",
                                    "status_reason_information": [],
                                    "acceptance_datetime": "2022-05-17T13:40:02Z",
                                    "effective_interbank_settlement_date": "2022-05-17T13:40:03.273Z",
                                    "clearing_system_reference": "C1DyRvKua6zu",
                                    "original_transaction_reference": {
                                        "amount": {
                                            "instructed_amount": {
                                                "amount": 2,
                                                "currency": "CAD"
                                            }
                                        },
                                        "remittance_information": {
                                            "structured": [
                                                {
                                                    "referred_document_information": [
                                                        {
                                                            "type": {
                                                                "code_or_proprietary": {
                                                                    "code": "MSIN"
                                                                }
                                                            },
                                                            "number": "23423400",
                                                            "related_date": "2022-05-17"
                                                        }
                                                    ],
                                                    "referred_document_amount": {
                                                        "due_payable_amount": {
                                                            "amount": 2,
                                                            "currency": "CAD"
                                                        },
                                                        "adjustment_amount_and_reason": [
                                                            {
                                                                "amount": {
                                                                    "amount": 2,
                                                                    "currency": "CAD"
                                                                },
                                                                "credit_debit_indicator": "CRDT",
                                                                "reason": "DUE"
                                                            }
                                                        ],
                                                        "remitted_amount": {
                                                            "amount": 2,
                                                            "currency": "CAD"
                                                        }
                                                    },
                                                    "creditor_reference_information": {
                                                        "type": {
                                                            "code_or_proprietary": {
                                                                "code": "RADP"
                                                            }
                                                        },
                                                        "reference": "string"
                                                    },
                                                    "additional_remittance_information": [
                                                        "test"
                                                    ]
                                                }
                                            ]
                                        },
                                        "debtor": {
                                            "name": "Test Debtor Corp Inc.",
                                            "postal_address": {
                                                "address_type": {},
                                                "post_code": "M1L 4S9",
                                                "town_name": "SCARBOROUGH",
                                                "country_sub_division": "Ontario",
                                                "country": "CA",
                                                "address_line": [
                                                    "2001 EGLINTON AVENUE E"
                                                ]
                                            },
                                            "contact_details": {
                                                "mobile_number": "+1-416-555-1222",
                                                "email_address": "testdebtor@company.com"
                                            }
                                        },
                                        "debtor_account": {
                                            "identification": {
                                                "other": {
                                                    "identification": "002-80150-0000000"
                                                }
                                            }
                                        },
                                        "debtor_agent": {
                                            "financial_institution_identification": {
                                                "clearing_system_member_identification": {
                                                    "member_identification": "000001007"
                                                }
                                            }
                                        },
                                        "creditor_agent": {
                                            "financial_institution_identification": {
                                                "clearing_system_member_identification": {
                                                    "member_identification": "000001007"
                                                }
                                            }
                                        },
                                        "creditor": {
                                            "name": "Test Creditor Corp Inc."
                                        },
                                        "creditor_account": {
                                            "identification": {
                                                "other": {
                                                    "identification": "002-47696-0400000"
                                                }
                                            }
                                        }
                                    }
                                }
                            ]
                        },
                        "payment_status": "REALTIME_DEPOSIT_COMPLETED",
                        "payment_expiry_date": "2022-06-16T13:39:55Z",
                        "payment_authentication": {},
                        "account_holder_name": "Test Debtor Corp Inc.",
                        "language": "EN"
                    },
                    "notifications": []
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }

    private MockResponseResult fallbackResponse() {
        var body = """
                {
                    "data": {},
                    "notifications": []
                }
                """;
        return new MockResponseResult(200, body, HEADERS);
    }
}
