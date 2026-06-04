# Canada Onboarding — Complete API Flow

> Canada national onboarding with **document-type branch** (ID vs PASSPORT). Temporal
> signal-driven workflow. Same pattern as the Foreign flow but with an extra
> `select-document` step before upload.
>
> **Service:** `onboarding-workflow-service` (port `8089`)
> **Base path:** `/api/v1/onboarding/ca`
> **Controller:** [CanadaOnboardingController.java](../../services/onboarding-workflow-service/src/main/java/com/ksa/financing/onboarding/canada/adapter/rest/controller/CanadaOnboardingController.java)
> **Workflow ID format:** `canada-onboarding-{16-hex}` (derived from email — idempotent per email)

---

## Step → State → Next Action map

| # | Endpoint | Auth | Workflow step after success | `nextAction` |
|---|----------|------|------------------------------|--------------|
| 1 | `POST /initiate` | Public | `OTP_SENT` | `VERIFY_OTP` |
| 2 | `POST /verify-otp` | Public | `OTP_VERIFIED` | `SELECT_DOCUMENT` |
| 3 | `POST /select-document` | 🔒 JWT | `DOC_SELECTED` | `UPLOAD_DOCUMENT` |
| 4 | `POST /upload-document` | 🔒 JWT | `DOC_VERIFIED` | `CONFIRM_DATA` |
| 5 | `POST /confirm-data` | 🔒 JWT | `DOC_CONFIRMED` | `UPLOAD_SELFIE` |
| 6 | `POST /upload-selfie` | 🔒 JWT | `SELFIE_VERIFIED` | `SET_PIN` |
| 7 | `POST /set-pin` | 🔒 JWT | `PIN_SETUP` | `ENABLE_BIOMETRICS` |
| 8 | `POST /enable-biometrics` | 🔒 JWT | `COMPLETED` | `DASHBOARD` |
| 9 | `GET /status?email=` | Public | (read-only) | — |

**Step enum:** `INITIATED → OTP_SENT → OTP_VERIFIED → DOC_SELECTED → DOC_VERIFIED → DOC_CONFIRMED → SELFIE_VERIFIED → PIN_SETUP → BIOMETRICS_SETUP → COMPLETED` (+ `FAILED`)

**Authorization key** for all 🔒 endpoints: `@SecuredEndpoint(obj = "onboarding", act = "update")`
JWT comes from Step 2 (`verify-otp` returns `accessToken`).

**Difference vs Foreign flow:** Canada has Step 3 `select-document` (ID / PASSPORT) — Foreign is passport-only and skips it.

### Flow diagram
```
[1] initiate ─OTP─> [2] verify-otp ─JWT─> [3] select-document (ID|PASSPORT)
                                                    │
                                          [4] upload-document (Facia OCR)
                                                    │
   [8] biometrics <─ [7] set-pin <─ [6] upload-selfie <─ [5] confirm-data
        │                              (face match →
   COMPLETED → DASHBOARD               customer + wallet)
```

---

## Common conventions

**Headers**
| Header | Required | Purpose |
|--------|----------|---------|
| `Content-Type: application/json` | yes | — |
| `Authorization: Bearer {jwt}` | steps 3-8 | JWT from `verify-otp` |
| `X-Tenant-Id` | optional | defaults to `00000000-0000-0000-0000-000000000001` |
| `X-Device-Id`, `X-Latitude`, `X-Longitude`, `X-Client-Ip`, `X-User-Agent` | optional | device/audit + risk gate |

**OTP (DEV)** — mobile OTP = stub `123456`; email OTP = **real code** emailed via SMTP (random, Redis TTL).

**Every response** carries: `workflowId`, `status`, `currentStep`, `nextAction`, `timestamp`.
On a step failure (`failureReason != null`) the workflow stays on the previous step and HTTP is `400` — retry the same step.

---

## Step 1 — Initiate

`POST /api/v1/onboarding/ca/initiate`  → **201 Created**

Runs a pre-flow **risk + fraud gate** (velocity, blacklist, route) against the mobile. A non-PASS decision blocks/routes (`status: BLOCKED`, or `COMPLETE` with `routeTo` for login/re-onboarding).

**Request**
```json
{
  "email": "john.carter@example.com",
  "mobileNumber": "+14165551234"
}
```

**Response**
```json
{
  "data": {
    "workflowId": "canada-onboarding-9f12ac0048d31b77",
    "status": "OTP_SENT",
    "currentStep": "OTP_SENT",
    "nextAction": "VERIFY_OTP",
    "mobileOtpRequestId": "mob-otp-551642be-474a-47ca-a016-13d64e89d96f",
    "emailOtpRequestId": "em-otp-813dfac2-c890-4632-86cf-59fc2833af54",
    "maskedMobile": "****1234",
    "maskedEmail": "j***r@example.com",
    "failureReason": null,
    "timestamp": "2026-06-02T08:10:06.537Z"
  },
  "message": "success"
}
```

**Status after this step** — `GET /status?email=…`
```json
{
  "data": {
    "workflowId": "canada-onboarding-9f12ac0048d31b77",
    "status": "OTP_SENT",
    "currentStep": "OTP_SENT",
    "nextAction": "VERIFY_OTP",
    "email": "john.carter@example.com",
    "mobileNumber": "+14165551234",
    "documentType": null,
    "customerId": null,
    "walletId": null,
    "keycloakUserId": null,
    "globalUid": null,
    "faceMatchScore": null,
    "pinSet": false,
    "biometricsEnabled": false,
    "extractedData": {},
    "confirmedData": {},
    "failureReason": null,
    "startedAt": "2026-06-02T08:10:02.632Z",
    "lastUpdatedAt": "2026-06-02T08:10:06.333Z"
  },
  "message": "success"
}
```

---

## Step 2 — Verify dual OTP

`POST /api/v1/onboarding/ca/verify-otp`  → **200 OK** (both verified) / **400** (retry)

Verifies mobile + email OTP in one call → creates Keycloak user → returns **JWT**.

**Request**
```json
{
  "email": "john.carter@example.com",
  "mobileOtp": "123456",
  "emailOtp": "839201"
}
```

**Response (200)**
```json
{
  "data": {
    "workflowId": "canada-onboarding-9f12ac0048d31b77",
    "status": "OTP_VERIFIED",
    "currentStep": "OTP_VERIFIED",
    "nextAction": "SELECT_DOCUMENT",
    "keycloakUserId": "38a42b26-823f-40cd-bf1a-342394fd8f43",
    "accessToken": "eyJhbGciOi…",
    "refreshToken": "eyJhbGciOi…",
    "expiresIn": 300,
    "tokenType": "Bearer",
    "failureReason": null,
    "mobileNumber": "+14165551234"
  },
  "message": "success"
}
```
> Use `accessToken` as `Authorization: Bearer …` for all remaining steps.

**Status after this step** — `GET /status?email=…`
```json
{
  "data": {
    "workflowId": "canada-onboarding-9f12ac0048d31b77",
    "status": "OTP_VERIFIED",
    "currentStep": "OTP_VERIFIED",
    "nextAction": "SELECT_DOCUMENT",
    "email": "john.carter@example.com",
    "mobileNumber": "+14165551234",
    "documentType": null,
    "customerId": null,
    "walletId": null,
    "keycloakUserId": "38a42b26-823f-40cd-bf1a-342394fd8f43",
    "globalUid": null,
    "faceMatchScore": null,
    "pinSet": false,
    "biometricsEnabled": false,
    "extractedData": {},
    "confirmedData": {},
    "failureReason": null,
    "startedAt": "2026-06-02T08:10:02.632Z",
    "lastUpdatedAt": "2026-06-02T08:10:20.110Z"
  },
  "message": "success"
}
```

---

## Step 3 — Select document type 🔒

`POST /api/v1/onboarding/ca/select-document`  → **200 OK**

Choose which document to upload. `documentType` = `ID` or `PASSPORT`.

**Request**
```json
{
  "email": "john.carter@example.com",
  "documentType": "ID"
}
```

**Response** → `currentStep: DOC_SELECTED`, `nextAction: UPLOAD_DOCUMENT`.

**Status after this step** — `GET /status?email=…`
```json
{
  "data": {
    "workflowId": "canada-onboarding-9f12ac0048d31b77",
    "status": "DOC_SELECTED",
    "currentStep": "DOC_SELECTED",
    "nextAction": "UPLOAD_DOCUMENT",
    "email": "john.carter@example.com",
    "mobileNumber": "+14165551234",
    "documentType": "ID",
    "customerId": null,
    "walletId": null,
    "keycloakUserId": "38a42b26-823f-40cd-bf1a-342394fd8f43",
    "globalUid": null,
    "faceMatchScore": null,
    "pinSet": false,
    "biometricsEnabled": false,
    "extractedData": {},
    "confirmedData": {},
    "failureReason": null,
    "startedAt": "2026-06-02T08:10:02.632Z",
    "lastUpdatedAt": "2026-06-02T08:10:48.300Z"
  },
  "message": "success"
}
```

---

## Step 4 — Upload document (Facia OCR) 🔒

`POST /api/v1/onboarding/ca/upload-document`  → **200 OK** / **400** (retry)

Sends ID/passport image to Facia for OCR + authenticity. Extracted fields returned in `extractedData`.

**Request**
```json
{
  "email": "john.carter@example.com",
  "documentImageBase64": "<base64 image>"
}
```

**Response (200)**
```json
{
  "data": {
    "workflowId": "canada-onboarding-9f12ac0048d31b77",
    "status": "DOC_VERIFIED",
    "currentStep": "DOC_VERIFIED",
    "nextAction": "CONFIRM_DATA",
    "faciaReferenceId": "facia-…",
    "extractedData": {
      "full_name": "John Carter",
      "dob": "1991-04-22",
      "document_number": "C1234567",
      "nationality": "CAN",
      "expiry_date": "2031-04-22",
      "issue_date": "2021-04-22",
      "selected_type": "id_card"
    },
    "message": "Document verified",
    "failureReason": null
  },
  "message": "success"
}
```

**Status after this step** — `GET /status?email=…`
```json
{
  "data": {
    "workflowId": "canada-onboarding-9f12ac0048d31b77",
    "status": "DOC_VERIFIED",
    "currentStep": "DOC_VERIFIED",
    "nextAction": "CONFIRM_DATA",
    "email": "john.carter@example.com",
    "mobileNumber": "+14165551234",
    "documentType": "ID",
    "customerId": null,
    "walletId": null,
    "keycloakUserId": "38a42b26-823f-40cd-bf1a-342394fd8f43",
    "globalUid": null,
    "faceMatchScore": null,
    "pinSet": false,
    "biometricsEnabled": false,
    "extractedData": {
      "full_name": "John Carter",
      "dob": "1991-04-22",
      "document_number": "C1234567",
      "nationality": "CAN",
      "expiry_date": "2031-04-22",
      "issue_date": "2021-04-22",
      "selected_type": "id_card"
    },
    "confirmedData": {},
    "failureReason": null,
    "startedAt": "2026-06-02T08:10:02.632Z",
    "lastUpdatedAt": "2026-06-02T08:11:05.221Z"
  },
  "message": "success"
}
```

---

## Step 5 — Confirm / correct extracted data 🔒

`POST /api/v1/onboarding/ca/confirm-data`  → **200 OK**

User confirms or corrects the OCR fields.

**Request**
```json
{
  "email": "john.carter@example.com",
  "surname": "Carter",
  "givenName": "John",
  "nationality": "CAN",
  "dateOfBirth": "1991-04-22",
  "documentNumber": "C1234567",
  "homeAddress": "55 Front St, Toronto, ON"
}
```
Required: `email, surname, givenName, nationality, dateOfBirth, documentNumber`.
Optional: `homeAddress`.

**Response** → `currentStep: DOC_CONFIRMED`, `nextAction: UPLOAD_SELFIE`.

**Status after this step** — `GET /status?email=…`
```json
{
  "data": {
    "workflowId": "canada-onboarding-9f12ac0048d31b77",
    "status": "DOC_CONFIRMED",
    "currentStep": "DOC_CONFIRMED",
    "nextAction": "UPLOAD_SELFIE",
    "email": "john.carter@example.com",
    "mobileNumber": "+14165551234",
    "documentType": "ID",
    "customerId": null,
    "walletId": null,
    "keycloakUserId": "38a42b26-823f-40cd-bf1a-342394fd8f43",
    "globalUid": null,
    "faceMatchScore": null,
    "pinSet": false,
    "biometricsEnabled": false,
    "extractedData": { "…": "…" },
    "confirmedData": {
      "surname": "Carter",
      "givenName": "John",
      "nationality": "CAN",
      "dateOfBirth": "1991-04-22",
      "documentNumber": "C1234567",
      "homeAddress": "55 Front St, Toronto, ON"
    },
    "failureReason": null,
    "startedAt": "2026-06-02T08:10:02.632Z",
    "lastUpdatedAt": "2026-06-02T08:11:40.500Z"
  },
  "message": "success"
}
```

---

## Step 6 — Upload selfie (face match → customer + wallet) 🔒

`POST /api/v1/onboarding/ca/upload-selfie`  → **200 OK** / **400** (retry)

Facia face-matches selfie vs the ID/passport. On success the workflow creates the **customer profile + wallet**.

**Request**
```json
{
  "email": "john.carter@example.com",
  "selfieImageBase64": "<base64 image>"
}
```

**Response (200)**
```json
{
  "data": {
    "workflowId": "canada-onboarding-9f12ac0048d31b77",
    "status": "SELFIE_VERIFIED",
    "currentStep": "SELFIE_VERIFIED",
    "nextAction": "SET_PIN",
    "faciaReferenceId": "facia-…",
    "faceMatchScore": 0.97,
    "customerId": "624e2c30-e7d0-417e-881a-b322df512185",
    "walletId": "6de264b1-ad58-4634-9e92-890ecc143550",
    "message": "Face match passed",
    "failureReason": null
  },
  "message": "success"
}
```

**Status after this step** — `GET /status?email=…`
```json
{
  "data": {
    "workflowId": "canada-onboarding-9f12ac0048d31b77",
    "status": "SELFIE_VERIFIED",
    "currentStep": "SELFIE_VERIFIED",
    "nextAction": "SET_PIN",
    "email": "john.carter@example.com",
    "mobileNumber": "+14165551234",
    "documentType": "ID",
    "customerId": "624e2c30-e7d0-417e-881a-b322df512185",
    "walletId": "6de264b1-ad58-4634-9e92-890ecc143550",
    "keycloakUserId": "38a42b26-823f-40cd-bf1a-342394fd8f43",
    "globalUid": "dd327591-8551-4fea-bf76-ee778b3216c4",
    "faceMatchScore": 0.97,
    "pinSet": false,
    "biometricsEnabled": false,
    "extractedData": { "…": "…" },
    "confirmedData": { "…": "…" },
    "failureReason": null,
    "startedAt": "2026-06-02T08:10:02.632Z",
    "lastUpdatedAt": "2026-06-02T08:12:10.880Z"
  },
  "message": "success"
}
```

> **Note:** The selfie step bumps `selfieAttempts` *before* creating customer+wallet and advancing to `SELFIE_VERIFIED`. The use case awaits the step transition so the response carries the real advanced state (was a race-bug — fixed 2026-06-02, same as Foreign flow).

---

## Step 7 — Set PIN 🔒

`POST /api/v1/onboarding/ca/set-pin`  → **200 OK** / **400**

**Request**
```json
{
  "email": "john.carter@example.com",
  "pin": "123456",
  "confirmPin": "123456"
}
```
`pin` = 6 digits, must equal `confirmPin`.

**Response** → `currentStep: PIN_SETUP`, `nextAction: ENABLE_BIOMETRICS`.

**Status after this step** — `GET /status?email=…`
```json
{
  "data": {
    "workflowId": "canada-onboarding-9f12ac0048d31b77",
    "status": "PIN_SETUP",
    "currentStep": "PIN_SETUP",
    "nextAction": "ENABLE_BIOMETRICS",
    "email": "john.carter@example.com",
    "mobileNumber": "+14165551234",
    "documentType": "ID",
    "customerId": "624e2c30-e7d0-417e-881a-b322df512185",
    "walletId": "6de264b1-ad58-4634-9e92-890ecc143550",
    "keycloakUserId": "38a42b26-823f-40cd-bf1a-342394fd8f43",
    "globalUid": "dd327591-8551-4fea-bf76-ee778b3216c4",
    "faceMatchScore": 0.97,
    "pinSet": true,
    "biometricsEnabled": false,
    "extractedData": { "…": "…" },
    "confirmedData": { "…": "…" },
    "failureReason": null,
    "startedAt": "2026-06-02T08:10:02.632Z",
    "lastUpdatedAt": "2026-06-02T08:12:35.140Z"
  },
  "message": "success"
}
```

---

## Step 8 — Enable biometrics (completes onboarding) 🔒

`POST /api/v1/onboarding/ca/enable-biometrics`  → **200 OK**

Enable Face ID / Touch ID, or skip. Either way this **completes** onboarding.

**Request**
```json
{
  "email": "john.carter@example.com",
  "enabled": true,
  "skipped": false
}
```

**Response**
```json
{
  "data": {
    "workflowId": "canada-onboarding-9f12ac0048d31b77",
    "status": "COMPLETED",
    "currentStep": "COMPLETED",
    "nextAction": "DASHBOARD",
    "message": "Onboarding complete",
    "failureReason": null
  },
  "message": "success"
}
```

**Status after this step** — `GET /status?email=…`
```json
{
  "data": {
    "workflowId": "canada-onboarding-9f12ac0048d31b77",
    "status": "COMPLETED",
    "currentStep": "COMPLETED",
    "nextAction": "DASHBOARD",
    "email": "john.carter@example.com",
    "mobileNumber": "+14165551234",
    "documentType": "ID",
    "customerId": "624e2c30-e7d0-417e-881a-b322df512185",
    "walletId": "6de264b1-ad58-4634-9e92-890ecc143550",
    "keycloakUserId": "38a42b26-823f-40cd-bf1a-342394fd8f43",
    "globalUid": "dd327591-8551-4fea-bf76-ee778b3216c4",
    "faceMatchScore": 0.97,
    "pinSet": true,
    "biometricsEnabled": true,
    "extractedData": { "…": "…" },
    "confirmedData": { "…": "…" },
    "failureReason": null,
    "startedAt": "2026-06-02T08:10:02.632Z",
    "lastUpdatedAt": "2026-06-02T08:13:38.963Z"
  },
  "message": "success"
}
```

---

## Step 9 — Status (resume / poll)

`GET /api/v1/onboarding/ca/status?email=john.carter@example.com`  → **200 OK**

Returns full workflow state — use it to resume after app restart. Read `nextAction` to know which step to call next. Fields populate progressively:

| Field | Populated at |
|-------|--------------|
| `keycloakUserId` | OTP_VERIFIED |
| `documentType` | DOC_SELECTED |
| `extractedData` | DOC_VERIFIED |
| `confirmedData` | DOC_CONFIRMED |
| `customerId`, `walletId`, `globalUid`, `faceMatchScore` | SELFIE_VERIFIED |
| `pinSet` | PIN_SETUP |
| `biometricsEnabled` | COMPLETED |

---

## Error / retry behaviour

| Situation | HTTP | What happens |
|-----------|------|--------------|
| Wrong OTP | 400 | step stays `OTP_SENT`, `failureReason` set → retry verify-otp |
| Document rejected by Facia | 400 | stays `DOC_SELECTED` → retry upload-document |
| Face match fail | 400 | stays `DOC_CONFIRMED` → retry upload-selfie |
| PIN mismatch | 400 | stays `SELFIE_VERIFIED` → retry set-pin |
| Risk gate block (initiate) | 403 / 200 | `status: BLOCKED` or routed (`COMPLETE` + `routeTo`) |
| Missing JWT on 🔒 step | 401 | obtain token from verify-otp |
| No `onboarding:update` policy for role | 403 | add Casbin policy (see below) |

**Casbin policy** (Redis key `casbin:policies:{role}`) needed for steps 3-8:
```
{role}, onboarding, update, allow
```

---

## Quick cURL smoke (DEV)

```bash
BASE=http://localhost:8089/api/v1/onboarding/ca
EMAIL=john.carter@example.com

# 1. initiate
curl -s -X POST $BASE/initiate -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"mobileNumber\":\"+14165551234\"}"

# 2. verify-otp  (mobile stub 123456; email = real emailed code)
curl -s -X POST $BASE/verify-otp -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"mobileOtp\":\"123456\",\"emailOtp\":\"<EMAILED>\"}"
# → copy accessToken

JWT='Bearer eyJ…'
# 3. select-document  (ID | PASSPORT)
curl -s -X POST $BASE/select-document -H 'Content-Type: application/json' -H "Authorization: $JWT" \
  -d "{\"email\":\"$EMAIL\",\"documentType\":\"ID\"}"

# 4. upload-document
curl -s -X POST $BASE/upload-document -H 'Content-Type: application/json' -H "Authorization: $JWT" \
  -d "{\"email\":\"$EMAIL\",\"documentImageBase64\":\"<b64>\"}"

# status (any time)
curl -s "$BASE/status?email=$EMAIL"
```

> Postman collection: [docs/postman/Canada-Onboarding.postman_collection.json](../postman/Canada-Onboarding.postman_collection.json)
> Related: [Foreign onboarding flow](FOREIGN_ONBOARDING_FLOW.md)
