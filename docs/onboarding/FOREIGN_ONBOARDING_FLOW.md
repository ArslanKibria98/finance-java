# Foreign National Onboarding — Complete API Flow

> Country-agnostic **passport-only** onboarding (no document-type selection). Captures
> country of origin + country of residence at initiate. Temporal signal-driven workflow.
>
> **Service:** `onboarding-workflow-service` (port `8089`)
> **Base path:** `/api/v1/onboarding/foreign`
> **Controller:** [ForeignOnboardingController.java](../../services/onboarding-workflow-service/src/main/java/com/ksa/financing/onboarding/foreign/adapter/rest/controller/ForeignOnboardingController.java)
> **Workflow ID format:** `foreign-onboarding-{16-hex}` (derived from email — idempotent per email)

---

## Step → State → Next Action map

| # | Endpoint | Auth | Workflow step after success | `nextAction` |
|---|----------|------|------------------------------|--------------|
| 1 | `POST /initiate` | Public | `OTP_SENT` | `VERIFY_OTP` |
| 2 | `POST /verify-otp` | Public | `OTP_VERIFIED` | `UPLOAD_PASSPORT` |
| 3 | `POST /upload-passport` | 🔒 JWT | `PASSPORT_UPLOADED` | `CONFIRM_DATA` |
| 4 | `POST /confirm-data` | 🔒 JWT | `DATA_CONFIRMED` | `UPLOAD_SELFIE` |
| 5 | `POST /upload-selfie` | 🔒 JWT | `SELFIE_VERIFIED` | `SET_PIN` |
| 6 | `POST /set-pin` | 🔒 JWT | `PIN_SETUP` | `ENABLE_BIOMETRICS` |
| 7 | `POST /enable-biometrics` | 🔒 JWT | `COMPLETED` | `DASHBOARD` |
| 8 | `GET /status?email=` | Public | (read-only) | — |

**Step enum:** `INITIATED → OTP_SENT → OTP_VERIFIED → PASSPORT_UPLOADED → DATA_CONFIRMED → SELFIE_VERIFIED → PIN_SETUP → BIOMETRICS_SETUP → COMPLETED` (+ `FAILED`)

**Authorization key** for all 🔒 endpoints: `@SecuredEndpoint(obj = "onboarding", act = "update")`
JWT comes from Step 2 (`verify-otp` returns `accessToken`).

### Flow diagram
```
[1] initiate ──OTP sent──> [2] verify-otp ──JWT issued──> [3] upload-passport (Facia OCR)
                                                                    │
   [7] enable-biometrics <── [6] set-pin <── [5] upload-selfie <── [4] confirm-data
        │                                       (face match →
   COMPLETED → DASHBOARD                         customer + wallet)
```

---

## Common conventions

**Headers**
| Header | Required | Purpose |
|--------|----------|---------|
| `Content-Type: application/json` | yes | — |
| `Authorization: Bearer {jwt}` | steps 3-7 | JWT from `verify-otp` |
| `X-Tenant-Id` | optional | defaults to `00000000-0000-0000-0000-000000000001` |
| `X-Device-Id`, `X-Latitude`, `X-Longitude`, `X-Client-Ip`, `X-User-Agent` | optional | device/audit + risk gate |

**OTP (DEV)** — mobile OTP = stub `123456`; email OTP = **real code** emailed via SMTP (random, Redis TTL).

**Every response** carries: `workflowId`, `status`, `currentStep`, `nextAction`, `timestamp`.
On a step failure (`failureReason != null`) the workflow stays on the previous step and HTTP is `400` — just retry the same step.

---

## Step 1 — Initiate

`POST /api/v1/onboarding/foreign/initiate`  → **201 Created**

Runs a pre-flow **risk + fraud gate** (velocity, blacklist, route) against the mobile. A non-PASS decision blocks/ routes (`status: BLOCKED`, or `COMPLETE` with `routeTo` for login/re-onboarding).

**Request**
```json
{
  "email": "momin.baig90@xintsolutions.com",
  "mobileNumber": "+923126461777",
  "countryOfOrigin": "PK",
  "residentialCountry": "SA"
}
```

**Response**
```json
{
  "data": {
    "workflowId": "foreign-onboarding-1e21aa024e879ea1",
    "status": "OTP_SENT",
    "currentStep": "OTP_SENT",
    "nextAction": "VERIFY_OTP",
    "mobileOtpRequestId": "mob-otp-551642be-474a-47ca-a016-13d64e89d96f",
    "emailOtpRequestId": "em-otp-813dfac2-c890-4632-86cf-59fc2833af54",
    "maskedMobile": "****1777",
    "maskedEmail": "m***0@xintsolutions.com",
    "failureReason": null,
    "timestamp": "2026-06-02T07:07:06.537Z"
  },
  "message": "success"
}
```

**Status after this step** — `GET /status?email=…`
```json
{
  "data": {
    "workflowId": "foreign-onboarding-1e21aa024e879ea1",
    "status": "OTP_SENT",
    "currentStep": "OTP_SENT",
    "nextAction": "VERIFY_OTP",
    "email": "momin.baig90@xintsolutions.com",
    "mobileNumber": "+923126461777",
    "countryOfOrigin": null,
    "residentialCountry": null,
    "customerId": null,
    "walletId": null,
    "keycloakUserId": null,
    "globalUid": null,
    "faceMatchScore": null,
    "pinSet": false,
    "biometricsEnabled": false,
    "onboardingComplete": false,
    "extractedData": {},
    "confirmedData": {},
    "failureReason": null,
    "startedAt": "2026-06-02T07:07:02.632Z",
    "lastUpdatedAt": "2026-06-02T07:07:06.333Z"
  },
  "message": "success"
}
```

---

## Step 2 — Verify dual OTP

`POST /api/v1/onboarding/foreign/verify-otp`  → **200 OK** (both verified) / **400** (retry)

Verifies mobile + email OTP in one call → creates Keycloak user → returns **JWT**.

**Request**
```json
{
  "email": "momin.baig90@xintsolutions.com",
  "mobileOtp": "123456",
  "emailOtp": "839201"
}
```

**Response (200)**
```json
{
  "data": {
    "workflowId": "foreign-onboarding-1e21aa024e879ea1",
    "status": "OTP_VERIFIED",
    "currentStep": "OTP_VERIFIED",
    "nextAction": "UPLOAD_PASSPORT",
    "keycloakUserId": "…",
    "accessToken": "eyJhbGciOi…",
    "refreshToken": "eyJhbGciOi…",
    "expiresIn": 300,
    "tokenType": "Bearer",
    "failureReason": null,
    "mobileNumber": "+923126461777"
  },
  "message": "success"
}
```
> Use `accessToken` as `Authorization: Bearer …` for all remaining steps.

**Status after this step** — `GET /status?email=…`
```json
{
  "data": {
    "workflowId": "foreign-onboarding-1e21aa024e879ea1",
    "status": "OTP_VERIFIED",
    "currentStep": "OTP_VERIFIED",
    "nextAction": "UPLOAD_PASSPORT",
    "email": "momin.baig90@xintsolutions.com",
    "mobileNumber": "+923126461777",
    "countryOfOrigin": null,
    "residentialCountry": null,
    "customerId": null,
    "walletId": null,
    "keycloakUserId": "38a42b26-823f-40cd-bf1a-342394fd8f43",
    "globalUid": null,
    "faceMatchScore": null,
    "pinSet": false,
    "biometricsEnabled": false,
    "onboardingComplete": false,
    "extractedData": {},
    "confirmedData": {},
    "failureReason": null,
    "startedAt": "2026-06-02T07:07:02.632Z",
    "lastUpdatedAt": "2026-06-02T07:07:20.110Z"
  },
  "message": "success"
}
```

---

## Step 3 — Upload passport (Facia OCR) 🔒

`POST /api/v1/onboarding/foreign/upload-passport`  → **200 OK** / **400** (retry)

Sends passport image to Facia for OCR + authenticity. Extracted fields returned in `extractedData`.

**Request**
```json
{
  "email": "momin.baig90@xintsolutions.com",
  "passportImageBase64": "<base64 image>"
}
```

**Response (200)**
```json
{
  "data": {
    "workflowId": "foreign-onboarding-1e21aa024e879ea1",
    "status": "PASSPORT_UPLOADED",
    "currentStep": "PASSPORT_UPLOADED",
    "nextAction": "CONFIRM_DATA",
    "faciaReferenceId": "facia-…",
    "extractedData": {
      "surname": "BAIG",
      "givenName": "MOMIN",
      "nationality": "PAK",
      "dateOfBirth": "1990-…",
      "passportNumber": "AB123456",
      "issueDate": "…",
      "expiryDate": "…"
    },
    "message": "Passport verified",
    "failureReason": null
  },
  "message": "success"
}
```

**Status after this step** — `GET /status?email=…`
```json
{
  "data": {
    "workflowId": "foreign-onboarding-1e21aa024e879ea1",
    "status": "PASSPORT_UPLOADED",
    "currentStep": "PASSPORT_UPLOADED",
    "nextAction": "CONFIRM_DATA",
    "email": "momin.baig90@xintsolutions.com",
    "mobileNumber": "+923126461777",
    "countryOfOrigin": null,
    "residentialCountry": null,
    "customerId": null,
    "walletId": null,
    "keycloakUserId": "38a42b26-823f-40cd-bf1a-342394fd8f43",
    "globalUid": null,
    "faceMatchScore": null,
    "pinSet": false,
    "biometricsEnabled": false,
    "onboardingComplete": false,
    "extractedData": {
      "surname": "BAIG",
      "givenName": "MOMIN",
      "nationality": "PAK",
      "dateOfBirth": "1990-01-15",
      "passportNumber": "AB123456",
      "issueDate": "2020-01-01",
      "expiryDate": "2030-01-01"
    },
    "confirmedData": {},
    "failureReason": null,
    "startedAt": "2026-06-02T07:07:02.632Z",
    "lastUpdatedAt": "2026-06-02T07:08:05.221Z"
  },
  "message": "success"
}
```

---

## Step 4 — Confirm / correct extracted data 🔒

`POST /api/v1/onboarding/foreign/confirm-data`  → **200 OK**

User confirms or corrects the OCR fields. Mobile app may send `documentNumber` instead of `passportNumber` (alias accepted).

**Request**
```json
{
  "email": "momin.baig90@xintsolutions.com",
  "surname": "BAIG",
  "givenName": "MOMIN",
  "nationality": "PAK",
  "dateOfBirth": "1990-01-15",
  "passportNumber": "AB123456",
  "issueDate": "2020-01-01",
  "expiryDate": "2030-01-01",
  "homeAddress": "…optional…",
  "countryOfOrigin": "PK",
  "residentialCountry": "SA"
}
```
Required: `email, surname, givenName, nationality, dateOfBirth, passportNumber`.
Optional: `homeAddress, issueDate, expiryDate, countryOfOrigin, residentialCountry`.

**Response** → `currentStep: DATA_CONFIRMED`, `nextAction: UPLOAD_SELFIE`.

**Status after this step** — `GET /status?email=…`
```json
{
  "data": {
    "workflowId": "foreign-onboarding-1e21aa024e879ea1",
    "status": "DATA_CONFIRMED",
    "currentStep": "DATA_CONFIRMED",
    "nextAction": "UPLOAD_SELFIE",
    "email": "momin.baig90@xintsolutions.com",
    "mobileNumber": "+923126461777",
    "countryOfOrigin": "PK",
    "residentialCountry": "SA",
    "customerId": null,
    "walletId": null,
    "keycloakUserId": "38a42b26-823f-40cd-bf1a-342394fd8f43",
    "globalUid": null,
    "faceMatchScore": null,
    "pinSet": false,
    "biometricsEnabled": false,
    "onboardingComplete": false,
    "extractedData": {
      "surname": "BAIG",
      "givenName": "MOMIN",
      "nationality": "PAK",
      "dateOfBirth": "1990-01-15",
      "passportNumber": "AB123456",
      "issueDate": "2020-01-01",
      "expiryDate": "2030-01-01"
    },
    "confirmedData": {
      "surname": "BAIG",
      "givenName": "MOMIN",
      "nationality": "PAK",
      "dateOfBirth": "1990-01-15",
      "passportNumber": "AB123456",
      "issueDate": "2020-01-01",
      "expiryDate": "2030-01-01",
      "homeAddress": "123 King St, Riyadh",
      "countryOfOrigin": "PK",
      "residentialCountry": "SA"
    },
    "failureReason": null,
    "startedAt": "2026-06-02T07:07:02.632Z",
    "lastUpdatedAt": "2026-06-02T07:08:40.500Z"
  },
  "message": "success"
}
```

---

## Step 5 — Upload selfie (face match → customer + wallet) 🔒

`POST /api/v1/onboarding/foreign/upload-selfie`  → **200 OK** / **400** (retry)

Facia face-matches selfie vs passport. On success the workflow creates the **customer profile + wallet**.

**Request**
```json
{
  "email": "momin.baig90@xintsolutions.com",
  "selfieImageBase64": "<base64 image>"
}
```

**Response (200)**
```json
{
  "data": {
    "workflowId": "foreign-onboarding-1e21aa024e879ea1",
    "status": "SELFIE_VERIFIED",
    "currentStep": "SELFIE_VERIFIED",
    "nextAction": "SET_PIN",
    "faciaReferenceId": "facia-…",
    "faceMatchScore": 0.97,
    "customerId": "…",
    "walletId": "…",
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
    "workflowId": "foreign-onboarding-1e21aa024e879ea1",
    "status": "SELFIE_VERIFIED",
    "currentStep": "SELFIE_VERIFIED",
    "nextAction": "SET_PIN",
    "email": "momin.baig90@xintsolutions.com",
    "mobileNumber": "+923126461777",
    "countryOfOrigin": "PK",
    "residentialCountry": "SA",
    "customerId": "624e2c30-e7d0-417e-881a-b322df512185",
    "walletId": "6de264b1-ad58-4634-9e92-890ecc143550",
    "keycloakUserId": "38a42b26-823f-40cd-bf1a-342394fd8f43",
    "globalUid": "dd327591-8551-4fea-bf76-ee778b3216c4",
    "faceMatchScore": 0.97,
    "pinSet": false,
    "biometricsEnabled": false,
    "onboardingComplete": false,
    "extractedData": { "…": "…" },
    "confirmedData": { "…": "…" },
    "failureReason": null,
    "startedAt": "2026-06-02T07:07:02.632Z",
    "lastUpdatedAt": "2026-06-02T07:09:10.880Z"
  },
  "message": "success"
}
```

---

## Step 6 — Set PIN 🔒

`POST /api/v1/onboarding/foreign/set-pin`  → **200 OK** / **400**

**Request**
```json
{
  "email": "momin.baig90@xintsolutions.com",
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
    "workflowId": "foreign-onboarding-1e21aa024e879ea1",
    "status": "PIN_SETUP",
    "currentStep": "PIN_SETUP",
    "nextAction": "ENABLE_BIOMETRICS",
    "email": "momin.baig90@xintsolutions.com",
    "mobileNumber": "+923126461777",
    "countryOfOrigin": "PK",
    "residentialCountry": "SA",
    "customerId": "624e2c30-e7d0-417e-881a-b322df512185",
    "walletId": "6de264b1-ad58-4634-9e92-890ecc143550",
    "keycloakUserId": "38a42b26-823f-40cd-bf1a-342394fd8f43",
    "globalUid": "dd327591-8551-4fea-bf76-ee778b3216c4",
    "faceMatchScore": 0.97,
    "pinSet": true,
    "biometricsEnabled": false,
    "onboardingComplete": false,
    "extractedData": { "…": "…" },
    "confirmedData": { "…": "…" },
    "failureReason": null,
    "startedAt": "2026-06-02T07:07:02.632Z",
    "lastUpdatedAt": "2026-06-02T07:09:35.140Z"
  },
  "message": "success"
}
```

---

## Step 7 — Enable biometrics (completes onboarding) 🔒

`POST /api/v1/onboarding/foreign/enable-biometrics`  → **200 OK**

Enable Face ID / Touch ID, or skip. Either way this **completes** onboarding.

**Request**
```json
{
  "email": "momin.baig90@xintsolutions.com",
  "enabled": true,
  "skipped": false
}
```

**Response**
```json
{
  "data": {
    "workflowId": "foreign-onboarding-1e21aa024e879ea1",
    "status": "COMPLETED",
    "currentStep": "COMPLETED",
    "nextAction": "DASHBOARD",
    "onboardingComplete": true,
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
    "workflowId": "foreign-onboarding-1e21aa024e879ea1",
    "status": "COMPLETED",
    "currentStep": "COMPLETED",
    "nextAction": "DASHBOARD",
    "email": "momin.baig90@xintsolutions.com",
    "mobileNumber": "+923126461777",
    "countryOfOrigin": "PK",
    "residentialCountry": "SA",
    "customerId": "624e2c30-e7d0-417e-881a-b322df512185",
    "walletId": "6de264b1-ad58-4634-9e92-890ecc143550",
    "keycloakUserId": "38a42b26-823f-40cd-bf1a-342394fd8f43",
    "globalUid": "dd327591-8551-4fea-bf76-ee778b3216c4",
    "faceMatchScore": 0.97,
    "pinSet": true,
    "biometricsEnabled": true,
    "onboardingComplete": true,
    "extractedData": { "…": "…" },
    "confirmedData": { "…": "…" },
    "failureReason": null,
    "startedAt": "2026-06-02T07:07:02.632Z",
    "lastUpdatedAt": "2026-06-02T07:10:38.963Z"
  },
  "message": "success"
}
```

---

## Step 8 — Status (resume / poll)

`GET /api/v1/onboarding/foreign/status?email=momin.baig90@xintsolutions.com`  → **200 OK**

Returns full workflow state — use it to resume after app restart. Read `nextAction` to know which step to call next.

**Response (example — mid-flow)**
```json
{
  "data": {
    "workflowId": "foreign-onboarding-1e21aa024e879ea1",
    "status": "OTP_SENT",
    "currentStep": "OTP_SENT",
    "nextAction": "VERIFY_OTP",
    "email": "momin.baig90@xintsolutions.com",
    "mobileNumber": "+923126461777",
    "countryOfOrigin": null,
    "residentialCountry": null,
    "customerId": null,
    "walletId": null,
    "keycloakUserId": null,
    "globalUid": null,
    "faceMatchScore": null,
    "pinSet": false,
    "biometricsEnabled": false,
    "onboardingComplete": false,
    "extractedData": {},
    "confirmedData": {},
    "failureReason": null,
    "startedAt": "2026-06-02T07:07:02.632Z",
    "lastUpdatedAt": "2026-06-02T07:07:06.333Z"
  },
  "message": "success"
}
```

---

## Error / retry behaviour

| Situation | HTTP | What happens |
|-----------|------|--------------|
| Wrong OTP | 400 | step stays `OTP_SENT`, `failureReason` set → retry verify-otp |
| Passport rejected by Facia | 400 | stays `OTP_VERIFIED` → retry upload-passport |
| Face match fail | 400 | stays `DATA_CONFIRMED` → retry upload-selfie |
| PIN mismatch | 400 | stays `SELFIE_VERIFIED` → retry set-pin |
| Risk gate block (initiate) | 403 / 200 | `status: BLOCKED` or routed (`COMPLETE` + `routeTo`) |
| Missing JWT on 🔒 step | 401 | obtain token from verify-otp |
| No `onboarding:update` policy for role | 403 | add Casbin policy (see below) |

**Casbin policy** (Redis key `casbin:policies:{role}`) needed for steps 3-7:
```
{role}, onboarding, update, allow
```

---

## Quick cURL smoke (DEV)

```bash
BASE=http://localhost:8089/api/v1/onboarding/foreign
EMAIL=momin.baig90@xintsolutions.com

# 1. initiate
curl -s -X POST $BASE/initiate -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"mobileNumber\":\"+923126461777\",\"countryOfOrigin\":\"PK\",\"residentialCountry\":\"SA\"}"

# 2. verify-otp  (mobile stub 123456; email = real emailed code)
curl -s -X POST $BASE/verify-otp -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"mobileOtp\":\"123456\",\"emailOtp\":\"<EMAILED>\"}"
# → copy accessToken

JWT='Bearer eyJ…'
# 3-7 use:  -H "Authorization: $JWT"
curl -s -X POST $BASE/upload-passport -H 'Content-Type: application/json' -H "Authorization: $JWT" \
  -d "{\"email\":\"$EMAIL\",\"passportImageBase64\":\"<b64>\"}"

# status (any time)
curl -s "$BASE/status?email=$EMAIL"
```

> Postman collection: [docs/postman/Foreign-National-Onboarding.postman_collection.json](../postman/Foreign-National-Onboarding.postman_collection.json)
