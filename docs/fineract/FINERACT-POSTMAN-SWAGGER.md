# Apache Fineract API - Postman Collections & Swagger UI

## 🎯 Swagger UI URL

### Your Instance
```
https://46.62.226.94:8443/fineract-provider/swagger-ui/index.html
```

**Access it directly in your browser!** ✅

### Alternative Demo Instances (Public)
If you want to explore a working example:
- **Official Demo**: https://demo.fineract.dev/fineract-provider/swagger-ui/index.html
- **Mifos Sandbox**: https://sandbox.mifos.community/fineract-provider/swagger-ui/index.html

---

## 📦 Postman Collections

### ⚠️ Important Note
**Apache Fineract 1.x does NOT have an official pre-built Postman collection.** The Postman collections you'll find are for **Fineract CN** (Cloud Native), which is a different, now-deprecated architecture.

### Option 1: Import from Swagger UI (Recommended for Fineract 1.x)

1. **Access Swagger UI**:
   ```
   https://46.62.226.94:8443/fineract-provider/swagger-ui/index.html
   ```

2. **Get OpenAPI Spec URL**:
   ```
   https://46.62.226.94:8443/fineract-provider/api-docs
   ```

3. **Import to Postman**:
   - Open Postman
   - Click **Import** → **Link**
   - Paste: `https://46.62.226.94:8443/fineract-provider/api-docs`
   - Click **Continue** → **Import**

   **Note**: You may need to disable SSL verification in Postman since we're using self-signed certificates.

### Option 2: Fineract CN Postman Collection (Different Architecture)

If you want to see examples from Fineract CN (note: this is NOT the same as Fineract 1.x):

**Repository**: https://github.com/apache/fineract-cn-docker-compose

**Collections Available**:
1. **Initial Requests - Part 1**:
   ```
   https://raw.githubusercontent.com/apache/fineract-cn-docker-compose/master/postman_scripts/Fineract-CN-Initial-Requests_PART1.postman_collection.json
   ```

2. **Initial Requests - Part 2**:
   ```
   https://raw.githubusercontent.com/apache/fineract-cn-docker-compose/master/postman_scripts/Fineract-CN-Initial-Requests_PART2.postman_collection.json
   ```

3. **Environment File**:
   ```
   https://raw.githubusercontent.com/apache/fineract-cn-docker-compose/master/postman_scripts/Fineract-Cn-Initial-Setup-Environment.postman_environment.json
   ```

**To Download**:
```bash
# Download all collections
curl -O https://raw.githubusercontent.com/apache/fineract-cn-docker-compose/master/postman_scripts/Fineract-CN-Initial-Requests_PART1.postman_collection.json
curl -O https://raw.githubusercontent.com/apache/fineract-cn-docker-compose/master/postman_scripts/Fineract-CN-Initial-Requests_PART2.postman_collection.json
curl -O https://raw.githubusercontent.com/apache/fineract-cn-docker-compose/master/postman_scripts/Fineract-Cn-Initial-Setup-Environment.postman_environment.json
```

---

## 🔧 Creating Your Own Postman Collection

Since there's no official Postman collection for Fineract 1.x, here's how to create one:

### Method 1: Use Swagger UI Export
1. Access: `https://46.62.226.94:8443/fineract-provider/swagger-ui/index.html`
2. Copy the OpenAPI spec URL or download the JSON
3. Import into Postman

### Method 2: Manual Creation with Common Endpoints

Here's a basic Postman collection structure you can use:

**Base URL**: `https://46.62.226.94:8443/fineract-provider/api/v1`

**Authentication**:
- Type: Basic Auth
- Username: `mifos`
- Password: `password`

**Required Header**:
- Key: `Fineract-Platform-TenantId`
- Value: `default`

**Common Endpoints**:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/offices` | List all offices |
| POST | `/offices` | Create new office |
| GET | `/clients` | List all clients |
| POST | `/clients` | Create new client |
| GET | `/loans` | List all loans |
| POST | `/loans` | Create new loan |
| GET | `/savingsaccounts` | List savings accounts |
| POST | `/savingsaccounts` | Create savings account |
| GET | `/users` | List all users |
| GET | `/staff` | List all staff |
| GET | `/groups` | List all groups |
| GET | `/codes` | List all codes |
| GET | `/datatables` | List all data tables |

---

## 📝 Sample Postman Requests

### 1. Get All Offices
```json
GET https://46.62.226.94:8443/fineract-provider/api/v1/offices
Headers:
  Fineract-Platform-TenantId: default
  Authorization: Basic bWlmb3M6cGFzc3dvcmQ=
```

### 2. Create a Client
```json
POST https://46.62.226.94:8443/fineract-provider/api/v1/clients
Headers:
  Fineract-Platform-TenantId: default
  Authorization: Basic bWlmb3M6cGFzc3dvcmQ=
  Content-Type: application/json

Body:
{
  "officeId": 1,
  "firstname": "Ahmad",
  "lastname": "Abdullah",
  "externalId": "CLI-001",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en",
  "active": true,
  "activationDate": "11 February 2026"
}
```

### 3. Get Client Details
```json
GET https://46.62.226.94:8443/fineract-provider/api/v1/clients/{clientId}
Headers:
  Fineract-Platform-TenantId: default
  Authorization: Basic bWlmb3M6cGFzc3dvcmQ=
```

---

## 🔐 Authentication in Postman

### Setup Basic Auth
1. Go to **Authorization** tab in your request/collection
2. Select **Type**: `Basic Auth`
3. Enter:
   - **Username**: `mifos`
   - **Password**: `password`

### Add Required Headers
In **Headers** tab, add:
```
Fineract-Platform-TenantId: default
Content-Type: application/json
```

### Disable SSL Verification (for self-signed certificates)
1. Go to **Settings** (⚙️ icon)
2. Turn **OFF** "SSL certificate verification"

---

## 🌐 OpenAPI Specification

### Get the OpenAPI JSON
```bash
curl -k https://46.62.226.94:8443/fineract-provider/api-docs
```

### Save to File
```bash
curl -k https://46.62.226.94:8443/fineract-provider/api-docs > fineract-openapi.json
```

Then import this file into:
- Postman
- Insomnia
- Any OpenAPI-compatible tool

---

## 📚 Additional Resources

### Official Documentation
- **API Docs**: https://fineract.apache.org/docs/current/
- **GitHub**: https://github.com/apache/fineract
- **Wiki**: https://cwiki.apache.org/confluence/display/FINERACT/

### Community Examples
- **Fineract Client**: https://github.com/openMF/fineract-client
- **OpenAPI Spec (Community)**: https://gist.github.com/Grandolf49/7101c0dd473b76f18ae3e801b1be4bb4

---

## 🚀 Quick Start with Postman

1. **Open Postman**

2. **Create New Collection**: "Fineract API"

3. **Set Collection Variables**:
   - `baseUrl`: `https://46.62.226.94:8443/fineract-provider/api/v1`
   - `username`: `mifos`
   - `password`: `password`
   - `tenantId`: `default`

4. **Set Collection Authorization**:
   - Type: Basic Auth
   - Username: `{{username}}`
   - Password: `{{password}}`

5. **Set Collection Headers**:
   - `Fineract-Platform-TenantId`: `{{tenantId}}`

6. **Add Requests**:
   - Create requests using `{{baseUrl}}/offices`, etc.

7. **Disable SSL Verification** in Settings

8. **Test**: Run "Get All Offices" to verify connection

---

## ✅ Verification

Test your setup with this simple request:

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

If you see this response, your API is working perfectly! 🎉
