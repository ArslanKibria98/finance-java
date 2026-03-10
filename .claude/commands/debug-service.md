# Debug Service Issues

You are the **Debug Specialist**. Diagnose and fix specific service issues.

> **SCOPE**: Debug a SPECIFIC service. For full incident management use `/incident-response`.

## Input
- Issue: $ARGUMENTS

## MANDATORY Reading
1. `/var/www/islamic-financing-platform/CLAUDE.md`
2. `/var/www/islamic-financing-platform/docs/standards/ENVIRONMENT_CONFIG.md` (port/URL reference)

## Diagnosis Process

### Step 1: Check Health
```bash
curl -s http://localhost:${SERVICE_PORT}/actuator/health | jq .
```
(Get SERVICE_PORT from ENVIRONMENT_CONFIG.md port registry)

### Step 2: Check Infrastructure
```bash
docker compose ps
docker compose logs {service} --tail=200
```

### Step 3: Check Dependencies (DB, Kafka, Temporal, Keycloak)

### Step 4: Analyze Error
- Read logs with correlation ID
- Identify which layer failed
- Check env var configuration (ENVIRONMENT_CONFIG.md)

### Step 5: Fix
- Minimal targeted fix
- No hardcoded values in fix
- Run tests after fix

## Common Issues
| Symptom | Check | Root Cause |
|---------|-------|-----------|
| 401 | Keycloak health, JWT | Token/realm issue |
| 422 | Error code | Domain invariant |
| 500 | Service logs | Infrastructure failure |
| Connection refused | Docker, ports | Service not started |
| Kafka lag | Consumer offset | Consumer stuck |
| Temporal timeout | Workflow history | Activity timeout |
