# Product ↔ COA Field ↔ Account Mapping API

## Overview

This API manages the mapping between **Products** → **COA Fields** → **GL Accounts**.

### Flow

```
1. Select Product (Product ID)
   ↓
2. Get available COA Fields for that product
   ↓
3. For each COA Field:
   - Select a GL Account (Account ID)
   - Store mapping: ProductId + CoaFieldId + AccountId
```

## Database Schema

### Tables Created

| Table | Purpose |
|-------|---------|
| `coa_fields` | COA Field definitions (types: Asset, Liability, Income, Expense) |
| `product_coa_field_mappings` | Which COA fields are assigned to which products |
| `product_coa_account_mappings` | Final mapping: Product + CoaField + SelectedAccount |
| `product_coa_audit_log` | Audit trail of all changes |

### Data Model

```sql
product_coa_account_mappings {
    id (UUID)
    tenant_id (UUID)
    product_id (UUID)              -- Which product
    coa_field_id (UUID)            -- Which COA field
    account_id (UUID)              -- Selected GL Account
    account_code (VARCHAR)         -- Account code (e.g., "1010")
    status (ACTIVE/INACTIVE)
    notes (TEXT)
    created_at (TIMESTAMPTZ)
}
```

---

## API Endpoints

### 1. Get COA Fields for a Product

```http
GET /api/v1/products/{productId}/coa-fields
Authorization: Bearer {JWT_TOKEN}
```

**Response**: List of COA fields with currently assigned accounts

```json
[
  {
    "id": "uuid-field-assignment-1",
    "productId": "uuid-product",
    "coaFieldId": "uuid-field",
    "fieldCode": "ASSET_ACCOUNT",
    "fieldName": "Asset Account",
    "fieldNameAr": "اثاثیات اکاؤنٹ",
    "fieldType": "ASSET",
    "isMandatory": true,
    "assignedAccountId": "uuid-account",
    "assignedAccountCode": "1010",
    "assignedAccountName": "Bank Account",
    "status": "ACTIVE",
    "assignedAt": "2026-04-17T12:00:00Z"
  }
]
```

---

### 2. Assign Account to Product's COA Field

**Flow Step**: After user selects a Product → selects a COA Field → selects an Account

```http
POST /api/v1/products/{productId}/coa-fields/{fieldId}/accounts
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Request Body**:

```json
{
  "coaFieldId": "uuid-field",
  "accountId": "uuid-account",
  "accountCode": "1010",
  "notes": "Bank account for Murabaha disbursements"
}
```

**Response** (201 Created):

```json
{
  "id": "uuid-mapping",
  "productId": "uuid-product",
  "coaFieldId": "uuid-field",
  "fieldCode": "ASSET_ACCOUNT",
  "fieldName": "Asset Account",
  "fieldNameAr": "اثاثیات اکاؤنٹ",
  "fieldType": "ASSET",
  "isMandatory": true,
  "assignedAccountId": "uuid-account",
  "assignedAccountCode": "1010",
  "assignedAccountName": "Bank Account",
  "status": "ACTIVE",
  "assignedAt": "2026-04-17T12:45:00Z"
}
```

---

### 3. Get Assigned Account for a Product Field

```http
GET /api/v1/products/{productId}/coa-fields/{fieldId}/account
Authorization: Bearer {JWT_TOKEN}
```

**Response**: Single account assigned to that field

```json
{
  "id": "uuid-mapping",
  "productId": "uuid-product",
  "coaFieldId": "uuid-field",
  "fieldCode": "ASSET_ACCOUNT",
  "fieldName": "Asset Account",
  "fieldNameAr": "اثاثیات اکاؤنٹ",
  "fieldType": "ASSET",
  "isMandatory": true,
  "assignedAccountId": "uuid-account",
  "assignedAccountCode": "1010",
  "assignedAccountName": "Bank Account",
  "status": "ACTIVE",
  "assignedAt": "2026-04-17T12:45:00Z"
}
```

---

### 4. Unassign Account from Product Field

```http
DELETE /api/v1/products/{productId}/coa-fields/{fieldId}/account
Authorization: Bearer {JWT_TOKEN}
```

**Response**: 204 No Content

---

## Example Workflow

### Scenario: Setup Murabaha Product GL Accounts

```bash
# Step 1: Get COA fields assigned to Murabaha product
GET /api/v1/products/{murabaha-product-id}/coa-fields
# Returns: ASSET_ACCOUNT, INCOME_ACCOUNT, EXPENSE_ACCOUNT fields

# Step 2: For each field, select and assign an account
POST /api/v1/products/{murabaha-product-id}/coa-fields/{asset-field-id}/accounts
{
  "coaFieldId": "{asset-field-id}",
  "accountId": "{1010-bank-account-id}",
  "accountCode": "1010",
  "notes": "Bank account for disbursements"
}
# Returns: Mapping created

# Step 3: Assign income account
POST /api/v1/products/{murabaha-product-id}/coa-fields/{income-field-id}/accounts
{
  "coaFieldId": "{income-field-id}",
  "accountId": "{4010-profit-account-id}",
  "accountCode": "4010",
  "notes": "Murabaha profit income account"
}

# Step 4: Verify all assignments
GET /api/v1/products/{murabaha-product-id}/coa-fields
# Returns: All fields with their selected accounts
```

---

## COA Field Types (Sample)

| Code | Name | Description | Type |
|------|------|-------------|------|
| `ASSET_ACCOUNT` | Asset Account | GL Account for assets | ASSET |
| `LIABILITY_ACCOUNT` | Liability Account | GL Account for liabilities | LIABILITY |
| `INCOME_ACCOUNT` | Income/Profit Account | GL Account for income | INCOME |
| `EXPENSE_ACCOUNT` | Expense Account | GL Account for expenses | EXPENSE |
| `CASH_ACCOUNT` | Cash Account | GL Account for cash | ASSET |
| `PROFIT_ACCOUNT` | Murabaha Profit | GL Account for Murabaha profit | INCOME |

---

## Authorization

All endpoints require:
- `Authorization: Bearer {JWT_TOKEN}` header
- User must have role with access to `products.coa-fields`  resource
- Tenant isolation enforced via JWT `tenant_id` claim

### Required Permissions

| Endpoint | Permission | Action |
|----------|-----------|--------|
| GET /coa-fields | products.coa-fields | read |
| POST /coa-fields/.../accounts | products.coa-fields | create |
| DELETE /coa-fields/.../account | products.coa-fields | delete |

---

## Error Handling

### 404 Not Found
```json
{
  "timestamp": "2026-04-17T12:45:00Z",
  "status": 404,
  "code": "PRODUCT_COA_FIELD_NOT_FOUND",
  "message": "COA Field mapping not found for product",
  "traceId": "abc123"
}
```

### 409 Conflict
```json
{
  "timestamp": "2026-04-17T12:45:00Z",
  "status": 409,
  "code": "CONFLICT",
  "message": "Account already assigned to this product field",
  "traceId": "abc123"
}
```

---

## Database Migration

Flyway migration: **V11__create_product_coa_field_mapping.sql**

Creates:
- `coa_fields` table with 6 sample fields
- `product_coa_field_mappings` table
- `product_coa_account_mappings` table
- `product_coa_audit_log` table
- Required indexes for performance

---

## Testing

### Postman Collection

Import to Postman → **"16 - Product COA Fields"**

#### Test Steps

1. **Get fields for product**
   ```
   GET {{kongUrl}}/ledger-service/api/v1/products/{productId}/coa-fields
   ```

2. **Assign bank account to asset field**
   ```
   POST {{kongUrl}}/ledger-service/api/v1/products/{productId}/coa-fields/{fieldId}/accounts
   Body: { "coaFieldId": "...", "accountId": "...", "accountCode": "1010" }
   ```

3. **Verify assignment**
   ```
   GET {{kongUrl}}/ledger-service/api/v1/products/{productId}/coa-fields/{fieldId}/account
   ```

---

## Future Enhancements

- [ ] Bulk assign multiple accounts for a product
- [ ] Template-based field assignment (copy from another product)
- [ ] Validation that account types match field types
- [ ] GL reconciliation reports by product
- [ ] Multi-currency support per field

---

**Service**: ledger-service  
**Created**: 2026-04-17  
**Version**: 1.0.0
