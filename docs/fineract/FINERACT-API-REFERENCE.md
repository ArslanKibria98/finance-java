# Fineract API Reference

## ✅ Status
**Fineract is now successfully running with PostgreSQL!**

- **Health**: UP
- **Database**: PostgreSQL 18.1
- **Tenant**: default

## API Access

### Base URL
```
https://46.62.226.94:8443/fineract-provider
```

### Authentication
**Default Credentials:**
- Username: `mifos`
- Password: `password`

### Required Headers
All API requests must include:
```
Fineract-Platform-TenantId: default
```

## Example API Calls

### 1. Health Check (No Auth Required)
```bash
curl -k https://46.62.226.94:8443/fineract-provider/actuator/health
```

**Response:**
```json
{"status":"UP","groups":["liveness","readiness"]}
```

### 2. Get Offices
```bash
curl -k -u mifos:password \
  -H "Fineract-Platform-TenantId: default" \
  https://46.62.226.94:8443/fineract-provider/api/v1/offices
```

**Response:**
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

### 3. Get Clients
```bash
curl -k -u mifos:password \
  -H "Fineract-Platform-TenantId: default" \
  https://46.62.226.94:8443/fineract-provider/api/v1/clients
```

### 4. Get Loans
```bash
curl -k -u mifos:password \
  -H "Fineract-Platform-TenantId: default" \
  https://46.62.226.94:8443/fineract-provider/api/v1/loans
```

### 5. Get Savings Accounts
```bash
curl -k -u mifos:password \
  -H "Fineract-Platform-TenantId: default" \
  https://46.62.226.94:8443/fineract-provider/api/v1/savingsaccounts
```

## Common Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/offices` | GET | List all offices |
| `/api/v1/clients` | GET | List all clients |
| `/api/v1/loans` | GET | List all loans |
| `/api/v1/savingsaccounts` | GET | List all savings accounts |
| `/api/v1/users` | GET | List all users |
| `/api/v1/staff` | GET | List all staff |
| `/api/v1/groups` | GET | List all groups |
| `/actuator/health` | GET | Health check |

## API Documentation

Full API documentation is available at:
```
https://46.62.226.94:8443/fineract-provider/swagger-ui.html
```

## Browser Access

**Important**: When accessing through a browser, you'll see a 404 error on the root path:
```
https://46.62.226.94:8443/fineract-provider/
```

This is **normal behavior** - Fineract doesn't have a web UI at the root path. You need to access specific API endpoints or the Swagger UI.

### Accessing API from Browser

1. **Swagger UI (Recommended)**:
   ```
   https://46.62.226.94:8443/fineract-provider/swagger-ui.html
   ```

2. **Direct API Endpoint** (will prompt for credentials):
   ```
   https://46.62.226.94:8443/fineract-provider/api/v1/offices
   ```
   - Username: `mifos`
   - Password: `password`

## PostgreSQL Database Info

### Connection Details
- **Host**: postgres (internal) / 46.62.226.94 (external)
- **Port**: 5432
- **Username**: postgres
- **Password**: postgres

### Databases
1. **fineract_tenants** - Tenant management
2. **fineract_default** - Default tenant data (200+ tables)
3. **ksa_financing** - Main application database

### Verify Database
```bash
docker exec ksa-postgres psql -U postgres -d fineract_tenants -c "SELECT * FROM tenants;"
```

## Container Management

### Check Status
```bash
docker ps | grep -E "(ksa-postgres|ksa-fineract)"
```

### View Logs
```bash
docker logs ksa-fineract
docker logs ksa-postgres
```

### Restart Services
```bash
docker restart ksa-fineract
docker restart ksa-postgres
```

## Notes

- SSL is self-signed, use `-k` flag with curl
- All responses are in JSON format
- Tenant ID "default" is pre-configured
- Default office "Head Office" is created automatically
- Startup time: ~2-3 minutes after container start
