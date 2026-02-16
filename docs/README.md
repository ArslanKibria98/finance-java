# KSA Islamic Financing Platform - Documentation

Complete documentation for the KSA Islamic Financing Platform.

> **📖 [Documentation Organization Guide](DOCUMENTATION-STRUCTURE.md)** | **✅ [Organization Complete](ORGANIZATION-COMPLETE.md)**

## 📚 Documentation Structure

```
docs/
├── getting-started/          # Quick start guides and onboarding
├── infrastructure/           # Infrastructure setup and configuration
│   ├── local-development/   # Local development with Docker Compose
│   └── kubernetes/          # Kubernetes deployment configurations
├── deployment/              # Deployment guides and procedures
├── operations/              # Operations, monitoring, and maintenance
└── guides/                  # Development guides and best practices
```

## 🚀 Getting Started

Start here if you're new to the project:

- **[Quick Start Guide](getting-started/quick-start.md)** - Get up and running in 5 minutes
- **[Local Development Setup](infrastructure/local-development/README.md)** - Complete local environment setup

## 🏗️ Infrastructure

### Local Development
- **[Docker Compose Setup](infrastructure/local-development/README.md)** - Local development with Docker
- **[Setup Complete](infrastructure/local-development/setup-complete.md)** - What was created and how to use it
- **[Checklist](infrastructure/local-development/checklist.md)** - Verification and requirements

### Kubernetes
- **[ELK Stack Implementation Report](infrastructure/kubernetes/elk-implementation-report.md)** - ELK Stack on Kubernetes
- **Monitoring Stack** - Prometheus, Grafana, Jaeger on Kubernetes (coming soon)
- **Services Deployment** - Microservices deployment configurations (coming soon)

## 🚀 Deployment

Deployment guides for different environments:

- **Local Development** - See [Infrastructure: Local Development](#local-development)
- **Staging Environment** - Coming soon
- **Production Deployment** - Coming soon

## 📊 Operations

Operations and maintenance guides:

- **Monitoring and Observability** - Coming soon
- **Logging and Troubleshooting** - Coming soon
- **Backup and Recovery** - Coming soon
- **Performance Tuning** - Coming soon

## 📖 Development Guides

Best practices and development guides:

- **Coding Standards** - Coming soon
- **Testing Guidelines** - Coming soon
- **API Development** - Coming soon
- **Database Migrations** - Coming soon

## 🔧 Configuration Reference

- **[Environment Variables](.env.example)** - All environment variables
- **[Docker Compose](../docker-compose.yml)** - Local infrastructure configuration
- **[Logstash Pipeline](../infrastructure/logstash/pipeline/logstash.conf)** - Log processing
- **[Prometheus Config](../infrastructure/prometheus/prometheus.yml)** - Metrics collection

## 📋 Quick Links

### Helper Scripts
Located in `/scripts/`:
- `start-infra.sh` - Start all infrastructure services
- `stop-infra.sh` - Stop all services
- `logs.sh <service>` - View service logs
- `validate-setup.sh` - Validate setup

### Service URLs (Local Development)
- **Keycloak**: http://localhost:8080 (admin/admin)
- **Grafana**: http://localhost:3000 (admin/admin)
- **Kibana**: http://localhost:5601
- **Temporal UI**: http://localhost:8233
- **Jaeger UI**: http://localhost:16686
- **Prometheus**: http://localhost:9090
- **Fineract**: https://localhost:8443

## 🆘 Support

- **Issues**: Create an issue in the project repository
- **Questions**: Refer to specific documentation sections above
- **Emergency**: Follow the incident response procedures (coming soon)

## 📝 Contributing to Documentation

When adding new documentation:

1. **Organize by category**: Place files in appropriate subdirectories
2. **Use clear naming**: Use lowercase with hyphens (e.g., `quick-start.md`)
3. **Update index**: Add links to this README
4. **Cross-reference**: Link to related documents
5. **Keep it current**: Update docs when changing code

### Documentation Categories

- **getting-started/**: Onboarding, quick starts, first-time setup
- **infrastructure/**: Infrastructure setup, configuration, and deployment
- **deployment/**: Deployment procedures, CI/CD, environments
- **operations/**: Day-to-day operations, monitoring, troubleshooting
- **guides/**: Development guides, best practices, how-tos

## 📅 Documentation Status

| Section | Status | Last Updated |
|---------|--------|--------------|
| Getting Started | ✅ Complete | 2026-02-11 |
| Infrastructure - Local | ✅ Complete | 2026-02-11 |
| Infrastructure - Kubernetes | 🚧 In Progress | - |
| Deployment | 📝 Planned | - |
| Operations | 📝 Planned | - |
| Development Guides | 📝 Planned | - |

---

**Need help?** Start with the [Quick Start Guide](getting-started/quick-start.md) or [Local Development Setup](infrastructure/local-development/README.md).
