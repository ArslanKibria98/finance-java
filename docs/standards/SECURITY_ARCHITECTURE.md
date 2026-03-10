# Security Architecture - Single Source of Truth

> Reference: Blueprint `03_SECURITY_DATA_RESIDENCY.md`, `11_USER_ROLES_PERMISSIONS.md`

## Authentication Flow
```
Client → Kong Gateway → Service SecurityFilterChain
  1. Kong validates API key / rate limit
  2. Service validates JWT against Keycloak JWKS
  3. TenantContextFilter extracts tenant_id from JWT claims
  4. @PreAuthorize evaluates ABAC conditions
  5. Controller receives @AuthenticationPrincipal Jwt
```

## ABAC Model (Attribute-Based Access Control)

This platform uses ABAC (NOT simple RBAC). Access decisions are based on:
- **Subject attributes**: role, department, tenant_id, partner_id
- **Resource attributes**: owner, status, amount, tenant_id
- **Action attributes**: create, read, update, approve
- **Environment attributes**: IP, time, geo-location

### Role Hierarchy
| Role | Level | Approval Limit | Scope |
|------|-------|---------------|-------|
| SUPER_ADMIN | 0 | Unlimited | Cross-tenant |
| ADMIN | 1 | Unlimited | Own tenant |
| OPS_HEAD | 2 | <= 1M SAR | Own tenant |
| COMPLIANCE_OFFICER | 3 | Read-only | Own tenant |
| HEAD_OF_ACCOUNTS | 3 | GL operations | Own tenant |
| UNDERWRITER | 4 | <= 100K SAR | Assigned cases |
| CSA | 5 | Customer ops | Own tenant |
| PARTNER_ADMIN | 5 | Partner scope | Own partner |
| PARTNER_MANAGER | 6 | Lead management | Own partner |

### @PreAuthorize Examples
```java
// Simple role check
@PreAuthorize("hasRole('ADMIN')")

// ABAC: amount-based
@PreAuthorize("hasRole('OPS_HEAD') and #request.amount <= 1000000")

// ABAC: owner check
@PreAuthorize("hasRole('UNDERWRITER') and @loanAccessService.isAssigned(#id, authentication)")

// ABAC: tenant isolation
@PreAuthorize("@tenantAccessService.canAccess(#tenantId, authentication)")
```

### Maker-Checker Requirements
| Operation | Maker | Checker |
|-----------|-------|---------|
| GL journal entry | Accountant | Head of Accounts |
| System configuration | Admin | Super Admin |
| Loan rescheduling | Underwriter | OPS Head |
| Fee waiver | CSA | OPS Head |
| Partner onboarding | Partner Manager | Admin |

## Encryption Standards
| What | Standard | Implementation |
|------|----------|----------------|
| PII at rest | AES-256-GCM | pii-vault-service |
| Data in transit | TLS 1.3 | All endpoints |
| Key management | HashiCorp Vault + HSM | Production only |
| Password hashing | Argon2id | Keycloak |
| Token signing | RS256 | Keycloak JWT |
| ZATCA signing | ECDSA P-256 | compliance-localization-sdk |

## Data Classification
| Level | Examples | Handling |
|-------|---------|---------|
| TOP SECRET | Encryption keys, HSM seeds | Vault only, zero-copy |
| CONFIDENTIAL | PII (NID, salary, address) | Encrypted, PII vault only |
| INTERNAL | Loan amounts, status, scores | Encrypted at rest |
| PUBLIC | Product catalog, FAQs | No restriction |

## Session Management
- Stateless JWT (no server-side sessions)
- Access token TTL: 5 minutes
- Refresh token TTL: 30 minutes
- Idle timeout: 15 minutes (configurable via env var)
- Max concurrent sessions: 3 per user

## Audit Trail Requirements (SAMA)
Every financial operation records:
- **WHO**: userId, roles, IP address, user-agent
- **WHAT**: action, resource type, resource ID
- **WHEN**: ISO 8601 timestamp with timezone
- **WHERE**: service name, pod instance, correlationId
- **RESULT**: success/failure, before-state, after-state
- **Retention**: 7 years minimum, 10 years for financial records
- **Storage**: WORM-compliant (Write Once Read Many)
