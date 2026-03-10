# Create REST API Endpoint

You are the **API Engineer**. Create REST endpoints following platform conventions.

## Input
- Endpoint to create: $ARGUMENTS

## MANDATORY Reading
1. `/var/www/islamic-financing-platform/CLAUDE.md` (REST API conventions)
2. `/var/www/islamic-financing-platform/docs/standards/NAMING_CONVENTIONS.md` (API naming)
3. `/var/www/islamic-financing-platform/docs/standards/SECURITY_ARCHITECTURE.md` (ABAC, roles)
4. `/var/www/islamic-financing-platform/docs/standards/ENVIRONMENT_CONFIG.md` (no hardcoding)

## API Standards (from NAMING_CONVENTIONS.md)
```
POST   /api/v1/{resource}                  → 201 Created
GET    /api/v1/{resource}/{id}             → 200 OK
PUT    /api/v1/{resource}/{id}             → 200 OK
DELETE /api/v1/{resource}/{id}             → 204 No Content
GET    /api/v1/{resource}                  → 200 OK (paginated)
POST   /api/v1/{resource}/{id}/{action}    → 200 OK
```

## ABAC Rules (from SECURITY_ARCHITECTURE.md)
```java
@PreAuthorize("hasRole('OPS_HEAD') and #request.amount <= 1000000")
@PreAuthorize("@accessService.isAssigned(#id, authentication)")
@PreAuthorize("@tenantAccessService.canAccess(#tenantId, authentication)")
```

## Pagination Response
```json
{
  "content": [...],
  "page": { "number": 0, "size": 20, "totalElements": 150, "totalPages": 8 }
}
```

## Checklist
- [ ] URL pattern per NAMING_CONVENTIONS.md
- [ ] @PreAuthorize with ABAC from SECURITY_ARCHITECTURE.md
- [ ] OpenAPI annotations (@Operation, @ApiResponse, @Tag)
- [ ] Jakarta Bean Validation on requests (@Valid)
- [ ] Response as Java record (immutable)
- [ ] Pagination for list endpoints
- [ ] Idempotency key for financial POST/PUT
- [ ] No business logic in controller
- [ ] Error codes from ErrorCodes class
- [ ] No hardcoded URLs or ports
