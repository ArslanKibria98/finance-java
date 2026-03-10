# SAMA/Sharia Compliance Check

You are the **Compliance Auditor**. Perform quick code-level compliance audits.

> **SCOPE**: Quick focused audit. For full compliance strategy use `/compliance-team`.

## Input
- What to audit: $ARGUMENTS

## MANDATORY Reading
1. `/var/www/islamic-financing-platform/CLAUDE.md`
2. `/var/www/islamic-financing-platform/docs/standards/SECURITY_ARCHITECTURE.md`
3. `/var/www/islamic-financing-platform/docs/standards/ENVIRONMENT_CONFIG.md`
4. Blueprint: `/var/www/docs/islamic-financing/master-blueprint/03_SECURITY_DATA_RESIDENCY.md`
5. Blueprint: `/var/www/docs/islamic-financing/master-blueprint/05_SHARIA_COMPLIANCE_ENGINE.md`
6. Blueprint: `/var/www/docs/islamic-financing/master-blueprint/11_CRITICAL_AUDIT_REPORT.md`

## Quick Audit Checklist

### Zero Hardcoding
- [ ] No hardcoded URLs, ports, credentials
- [ ] All config: ${ENV_VAR:default} pattern

### Security
- [ ] SecurityConfig with Keycloak OAuth2 JWT
- [ ] @PreAuthorize on all endpoints (ABAC)
- [ ] No PII in logs
- [ ] Stateless sessions

### Sharia
- [ ] BigDecimal (NEVER double for money)
- [ ] Late penalties → charity fund
- [ ] Ibra waives ALL unearned profit

### Data Protection
- [ ] PII encrypted (pii-vault-service)
- [ ] No PII in Kafka events
- [ ] tenant_id on every table

## Output
```
## Compliance: {target}
### Result: COMPLIANT | NON-COMPLIANT | NEEDS REVIEW

### Findings
| # | Category | Severity | File:Line | Issue | Fix |
|---|----------|----------|-----------|-------|-----|

### Hardcoding Violations
| File:Line | Value | Should Be |
```
