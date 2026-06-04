-- =====================================================================
-- V24: Request templates — callers send a SIMPLE body, middleware expands
--      it into the EXACT provider body (provider_apis.request_template).
-- ---------------------------------------------------------------------
-- A template is the full provider JSON with the fixed structure baked in
-- and ${field} / ${field:default} placeholders for the few dynamic fields.
-- JsonTemplateRenderer substitutes the caller's flat fields (numbers stay
-- numbers); a missing field with no default omits that property.
--
-- Seeded for the complex-body Scotia APIs. GET/inquire/summary/submit have
-- no body → no template. Other providers are unaffected (template = NULL).
-- =====================================================================
ALTER TABLE provider_apis ADD COLUMN IF NOT EXISTS request_template JSONB;

DO $$
DECLARE
    v_tenant UUID := '00000000-0000-0000-0000-000000000001';
BEGIN
    -- ---------- EFT: Account Validation ----------
    -- simple: { accountNumber, institutionNumber, transit, fullName, currency?, activitySince? }
    UPDATE provider_apis SET request_template = $json$
    {
      "account_information": {
        "account_number": "${accountNumber}",
        "currency_code": "${currency:CAD}",
        "institution_number": "${institutionNumber}",
        "transit": "${transit}",
        "name": { "full_name": "${fullName}" },
        "transaction_activity_since": "${activitySince:2022-01-01}"
      }
    }
    $json$::jsonb
    WHERE tenant_id = v_tenant AND code = 'SCOTIABANK_ACCOUNT_VALIDATION';

    -- ---------- EFT: Create ----------
    -- simple: { amount, currency?, debtorName, debtorAccount, creditorName, creditorAccount,
    --           submissionId?, messageIdentification?, agreementId?, endToEndId?, executionDate? }
    UPDATE provider_apis SET request_template = $json$
    {
      "data": {
        "submission_id": "${submissionId:1000000001}",
        "message_identification": "${messageIdentification:1000000001}",
        "agreement_id": "${agreementId:SD123456789}",
        "number_of_transactions": "1",
        "payments": [
          {
            "payment_type": "EFT",
            "initiation": {
              "end_to_end_identification": "${endToEndId:2000000001}",
              "credit_debit_indicator": "CRDT",
              "requested_execution_date": "${executionDate:2023-07-12}",
              "instructed_amount": { "amount": "${amount}", "currency": "${currency:CAD}" },
              "debtor": { "name": "${debtorName}", "postal_address": { "town_name": "${debtorTown:Toronto}", "country_sub_division": "${debtorProvince:ON}", "country": "${debtorCountry:CA}", "post_code": "${debtorPostcode:M1234M}" } },
              "debtor_account": { "name": "${debtorName}", "identification": { "other": { "identification": "${debtorAccount}" } } },
              "creditor": { "name": "${creditorName}", "postal_address": { "town_name": "${creditorTown:Toronto}", "country_sub_division": "${creditorProvince:ON}", "country": "${creditorCountry:CA}", "post_code": "${creditorPostcode:M1234M}" } },
              "creditor_account": { "name": "${creditorName}", "identification": { "other": { "identification": "${creditorAccount}" } } }
            }
          }
        ]
      }
    }
    $json$::jsonb
    WHERE tenant_id = v_tenant AND code = 'SCOTIABANK_EFT_CREATE';

    -- ---------- RTP: Payment Options Inquiry ----------
    -- simple: { depositHandle, depositType?, productCode? }
    UPDATE provider_apis SET request_template = $json$
    {
      "product_code": "${productCode:DOMESTIC}",
      "deposit_type": "${depositType:ACCOUNT_DEPOSIT}",
      "deposit_handle": "${depositHandle}"
    }
    $json$::jsonb
    WHERE tenant_id = v_tenant AND code = 'SCOTIABANK_PAYMENT_OPTIONS_INQUIRY';

    -- ---------- RTP: Commit Transaction ----------
    -- simple: { amount, currency?, messageIdentification?, debtorName?, debtorEmail?, debtorAccount,
    --           creditorName?, creditorEmail?, creditorAccount }
    UPDATE provider_apis SET request_template = $json$
    {
      "initiation": {
        "product_code": "DOMESTIC",
        "message_identification": "${messageIdentification:2450779}",
        "number_of_transactions": 1,
        "credit_debit_indicator": "CRDT",
        "payment_type_identification": "REALTIME_ACCOUNT_DEPOSIT_PAYMENT",
        "language": "EN",
        "instructed_amount": { "amount": "${amount}", "currency": "${currency:CAD}" },
        "debtor": { "name": "${debtorName:Test Debtor Corp Inc.}", "contact_details": { "email_address": "${debtorEmail:testdebtor@company.com}" } },
        "debtor_account": { "identification": { "other": { "identification": "${debtorAccount}" } } },
        "creditor": { "name": "${creditorName:Test Creditor Corp Inc.}", "contact_details": { "email_address": "${creditorEmail:testcreditor@company.com}" } },
        "creditor_account": { "identification": { "other": { "identification": "${creditorAccount}", "scheme_name": { "proprietary": "BANK_ACCT_NO" } } } }
      }
    }
    $json$::jsonb
    WHERE tenant_id = v_tenant AND code = 'SCOTIABANK_PAYMENT_COMMIT';

    -- ---------- RTP: Cancel ----------
    -- simple: { reason? }
    UPDATE provider_apis SET request_template = $json$
    { "cancel_payment_reason": "${reason:cancel reason}" }
    $json$::jsonb
    WHERE tenant_id = v_tenant AND code = 'SCOTIABANK_PAYMENT_CANCEL';

    -- ---------- WIRE: Validate + Create (same body) ----------
    -- simple: { amount, currency?, debtorName, debtorAccount, creditorName, creditorAccount,
    --           messageIdentification?, instructionId? }
    UPDATE provider_apis SET request_template = $json$
    {
      "data": {
        "message_identification": "${messageIdentification:d9ff0bb9-5d6a-4b76-a2ac-29ac4378a11b}",
        "initiation": {
          "payment_identification": { "instruction_identification": "${instructionId:707550001671}", "end_to_end_identification": "${instructionId:707550001671}" },
          "payment_type_information": { "instruction_priority": "NORM", "service_level": [ { "code": "BKTR" } ], "local_instrument": { "code": "DDMC" } },
          "amount": { "instructed_amount": { "amount": "${amount}", "currency": "${currency:CAD}" } },
          "charge_bearer": "CRED",
          "debtor": { "name": "${debtorName}", "postal_address": { "town_name": "${debtorTown:Toronto}", "country_sub_division": "${debtorProvince:ON}", "country": "${debtorCountry:CA}", "post_code": "${debtorPostcode:M1N 2N0}" }, "country_of_residence": "CA" },
          "debtor_account": { "identification": { "other": { "identification": "${debtorAccount}", "scheme_name": { "code": "BBAN" } } }, "type": { "proprietary": "DDA" }, "currency": "${currency:CAD}" },
          "creditor": { "name": "${creditorName}", "postal_address": { "town_name": "${creditorTown:ABC Town}", "country_sub_division": "${creditorProvince:TG}", "country": "${creditorCountry:IN}", "post_code": "${creditorPostcode:M1T 3S5}" }, "country_of_residence": "CA" },
          "creditor_agent": { "financial_institution_identification": { "bicfi": "${creditorBic:ACCTCA61XXX}", "clearing_system_member_identification": { "clearing_system_identification": { "code": "CACPA" }, "member_identification": "${creditorMember:asdf}" } } },
          "creditor_account": { "identification": { "other": { "identification": "${creditorAccount}", "scheme_name": { "code": "BBAN" } } }, "type": { "code": "CACC" }, "currency": "${currency:CAD}" }
        }
      }
    }
    $json$::jsonb
    WHERE tenant_id = v_tenant AND code IN ('SCOTIABANK_WIRE_VALIDATE', 'SCOTIABANK_WIRE_CREATE');

    RAISE NOTICE 'Request templates seeded for 7 Scotia APIs (account-validation, EFT create, RTP options/commit/cancel, WIRE validate/create)';
END $$;
