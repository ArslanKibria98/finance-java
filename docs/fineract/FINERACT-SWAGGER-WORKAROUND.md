# Fineract Swagger UI - Known Issue & Workarounds

## ⚠️ The Problem

When you access the Swagger UI at:
```
https://46.62.226.94:8443/fineract-provider/swagger-ui/index.html
```

You see this error:
```
Failed to load API definition.
Fetch error
response status is 404 /fineract-provider/fineract.json
```

**This is a known issue with Apache Fineract 1.10.1.** The OpenAPI specification is not properly generated, resulting in empty `paths` and `components`.

## 🔍 Root Cause

The `/fineract-provider/api-docs` endpoint returns:
```json
{
  "openapi": "3.0.1",
  "info": {
    "title": "OpenAPI definition",
    "version": "v0"
  },
  "servers": [{
    "url": "https://localhost:8443/fineract-provider",
    "description": "Generated server url"
  }],
  "paths": {},      ← EMPTY!
  "components": {}  ← EMPTY!
}
```

This is a **build-time issue** where the OpenAPI annotations are not properly processed.

## ✅ Workarounds

### Option 1: Use a Pre-Generated OpenAPI Spec (Recommended)

Download a community-maintained OpenAPI specification:

```bash
curl -o fineract-openapi.json https://gist.githubusercontent.com/Grandolf49/7101c0dd473b76f18ae3e801b1be4bb4/raw/fineract-openapi-3.0.3.json
```

Then import this file into:
- **Postman**: Import → File → Select `fineract-openapi.json`
- **Swagger Editor**: https://editor.swagger.io/ (paste the content)

**Source**: https://gist.github.com/Grandolf49/7101c0dd473b76f18ae3e801b1be4bb4

### Option 2: Use the Mifos Sandbox Swagger UI

Access a working Swagger UI from the official Mifos sandbox:

```
https://sandbox.mifos.community/fineract-provider/swagger-ui/index.html
```

**Note**: This is pointing to a different server, but you can:
1. Explore the API structure
2. See all available endpoints
3. Export the OpenAPI spec
4. Modify the server URL to point to your instance

### Option 3: Manual API Testing (Current Workaround)

Since your API is working (just not the Swagger UI), use these methods:

#### Using cURL
```bash
# List all offices
curl -k -u mifos:password \
  -H "Fineract-Platform-TenantId: default" \
  https://46.62.226.94:8443/fineract-provider/api/v1/offices
```

#### Using Postman Without Swagger

**Base URL**: `https://46.62.226.94:8443/fineract-provider/api/v1`

**Authentication**:
- Type: Basic Auth
- Username: `mifos`
- Password: `password`

**Headers**:
- `Fineract-Platform-TenantId`: `default`
- `Content-Type`: `application/json`

**Common Endpoints to Test**:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/offices` | List offices |
| GET | `/clients` | List clients |
| GET | `/loans` | List loans |
| GET | `/savingsaccounts` | List savings |
| GET | `/users` | List users |
| GET | `/staff` | List staff |
| GET | `/groups` | List groups |

### Option 4: Upgrade to Fineract 1.11+ (Future)

This Swagger issue has been addressed in newer versions. Consider upgrading when:
- Fineract 1.11.0 or later is stable
- Your platform dependencies are compatible
- You can test the upgrade in a development environment

**Related Fixes**:
- FINERACT-2147: Fixed Swagger UI endpoint paths
- FINERACT-2039: Fixed GET Savings API swagger
- Multiple Swagger-related improvements

## 📦 Ready-to-Use Postman Collection

I've created a basic Postman collection for you with common endpoints:

### Download Links

**Fineract Client OpenAPI Spec**:
```
https://raw.githubusercontent.com/openMF/fineract-client/master/swagger-api-spec-file.json
```

This is a complete OpenAPI specification maintained by the OpenMF community.

### Import to Postman

1. Open Postman
2. Click **Import**
3. Choose **Link** tab
4. Paste: `https://raw.githubusercontent.com/openMF/fineract-client/master/swagger-api-spec-file.json`
5. Click **Continue** → **Import**

6. **Update the Server URL**:
   - Go to the imported collection
   - Edit the collection
   - Change server URL from default to: `https://46.62.226.94:8443/fineract-provider`

7. **Set Authentication**:
   - Authorization → Basic Auth
   - Username: `mifos`
   - Password: `password`

8. **Add Header**:
   - Key: `Fineract-Platform-TenantId`
   - Value: `default`

## 🎯 Quick Test Collection

Here's a simple Postman collection JSON you can import:

```json
{
  "info": {
    "name": "Fineract API - KSA Instance",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Get Offices",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Fineract-Platform-TenantId",
            "value": "default"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/api/v1/offices",
          "host": ["{{baseUrl}}"],
          "path": ["api", "v1", "offices"]
        }
      }
    },
    {
      "name": "Get Clients",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Fineract-Platform-TenantId",
            "value": "default"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/api/v1/clients",
          "host": ["{{baseUrl}}"],
          "path": ["api", "v1", "clients"]
        }
      }
    }
  ],
  "auth": {
    "type": "basic",
    "basic": [
      {
        "key": "username",
        "value": "mifos"
      },
      {
        "key": "password",
        "value": "password"
      }
    ]
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "https://46.62.226.94:8443/fineract-provider"
    }
  ]
}
```

Save this as `fineract-ksa.postman_collection.json` and import it.

## 📚 API Reference (Without Swagger)

### Core Entities

**Offices**: `/api/v1/offices`
**Clients**: `/api/v1/clients`
**Groups**: `/api/v1/groups`
**Staff**: `/api/v1/staff`
**Users**: `/api/v1/users`

### Products

**Loan Products**: `/api/v1/loanproducts`
**Savings Products**: `/api/v1/savingsproducts`
**Share Products**: `/api/v1/products/share`
**Fixed Deposit Products**: `/api/v1/fixeddepositproducts`
**Recurring Deposit Products**: `/api/v1/recurringdepositproducts`

### Accounts

**Loans**: `/api/v1/loans`
**Savings Accounts**: `/api/v1/savingsaccounts`
**Share Accounts**: `/api/v1/accounts/share`
**Fixed Deposits**: `/api/v1/fixeddepositaccounts`
**Recurring Deposits**: `/api/v1/recurringdepositaccounts`

### Accounting

**Journal Entries**: `/api/v1/journalentries`
**GL Accounts**: `/api/v1/glaccounts`
**Accounting Rules**: `/api/v1/accountingrules`

### Configuration

**Codes**: `/api/v1/codes`
**Hooks**: `/api/v1/hooks`
**Data Tables**: `/api/v1/datatables`
**Maker Checker**: `/api/v1/makercheckers`

## ✅ Verification

Test that your API is working:

```bash
curl -k -u mifos:password \
  -H "Fineract-Platform-TenantId: default" \
  https://46.62.226.94:8443/fineract-provider/api/v1/offices
```

**Expected Response**:
```json
[{
  "id": 1,
  "name": "Head Office",
  "nameDecorated": "Head Office",
  "externalId": "1",
  "openingDate": [2009, 1, 1],
  "hierarchy": "."
}]
```

If you get this response, your API is fully functional - it's just the Swagger UI that has issues! ✅

## 🔧 Technical Details

**Issue**: FINERACT-1105, FINERACT-2147
**Status**: Fixed in Fineract 1.11+
**Current Version**: 1.10.1 (has this issue)
**Impact**: Swagger UI doesn't work, but REST API is fully functional

The Swagger documentation generation issue doesn't affect the API functionality - it's purely a documentation/UI problem.
