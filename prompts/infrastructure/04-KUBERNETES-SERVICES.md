# ☸️ Infrastructure Prompt 04: Kubernetes Service Deployments (FINAL)

**Objective**: Deploy all 20 microservices + infrastructure to Kubernetes with production-ready configurations.

**Prerequisites**: ✅ All prompts 01-03 complete, all services built

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/02_TECHNOLOGY_STACK.md` - All configurations
- `/var/www/docs/islamic-financing/master-blueprint/09_OBSERVABILITY_OPERATIONS.md` - Production settings
- `/var/www/docs/islamic-financing/master-blueprint/03_SECURITY_DATA_RESIDENCY.md` - Security requirements

---

## 🎯 Implementation Requirements

### Directory Structure
```
infrastructure/kubernetes/
├── namespaces/
│   ├── ksa-financing-dev.yaml
│   ├── ksa-financing-staging.yaml
│   └── ksa-financing-prod.yaml
│
├── infrastructure/
│   ├── postgres/
│   │   ├── statefulset.yaml
│   │   └── service.yaml
│   ├── redis/
│   ├── kafka/
│   ├── temporal/
│   └── keycloak/
│
├── services/
│   ├── lending-service/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   ├── configmap.yaml
│   │   ├── secret.yaml
│   │   └── hpa.yaml
│   │
│   ├── customer-service/
│   │   └── ... (same structure)
│   │
│   └── ... (all 20 services)
│
├── ingress/
│   ├── api-gateway-ingress.yaml
│   └── internal-ingress.yaml
│
└── helm/
    └── ksa-financing-platform/
        ├── Chart.yaml
        ├── values.yaml
        ├── values-dev.yaml
        ├── values-staging.yaml
        └── values-prod.yaml
```

### Common Deployment Template

For each service, create:

#### 1. Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: lending-service
  namespace: ksa-financing-prod
  labels:
    app: lending-service
    version: v1
spec:
  replicas: 3
  selector:
    matchLabels:
      app: lending-service
  template:
    metadata:
      labels:
        app: lending-service
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      containers:
      - name: lending-service
        image: ksa-financing/lending-service:1.0.0
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
        - containerPort: 9090
          name: grpc
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            configMapKeyRef:
              name: lending-service-config
              key: database.url
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: lending-service-secrets
              key: database.password
        - name: KAFKA_BOOTSTRAP_SERVERS
          value: "kafka:9092"
        - name: TEMPORAL_HOST
          value: "temporal"
        - name: KEYCLOAK_SERVER_URL
          value: "https://keycloak.ksa-financing.com"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
```

#### 2. Service
```yaml
apiVersion: v1
kind: Service
metadata:
  name: lending-service
  namespace: ksa-financing-prod
spec:
  selector:
    app: lending-service
  ports:
  - name: http
    port: 8080
    targetPort: 8080
  - name: grpc
    port: 9090
    targetPort: 9090
  type: ClusterIP
```

#### 3. HorizontalPodAutoscaler
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: lending-service-hpa
  namespace: ksa-financing-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: lending-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

#### 4. ConfigMap
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: lending-service-config
  namespace: ksa-financing-prod
data:
  database.url: "jdbc:postgresql://postgres:5432/lending_service"
  logging.level: "INFO"
  temporal.namespace: "ksa-financing-prod"
```

#### 5. Secret
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: lending-service-secrets
  namespace: ksa-financing-prod
type: Opaque
stringData:
  database.password: "<encrypted>"
  keycloak.client.secret: "<encrypted>"
```

### Ingress Configuration

#### API Gateway Ingress
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: api-gateway
  namespace: ksa-financing-prod
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - api.ksa-financing.com
    secretName: api-tls-secret
  rules:
  - host: api.ksa-financing.com
    http:
      paths:
      - path: /api/v1/loans
        pathType: Prefix
        backend:
          service:
            name: lending-service
            port:
              number: 8080
      - path: /api/v1/customers
        pathType: Prefix
        backend:
          service:
            name: customer-service
            port:
              number: 8080
      # ... all other services
```

### Helm Chart

Create Helm chart for templating:

#### Chart.yaml
```yaml
apiVersion: v2
name: ksa-financing-platform
description: KSA Islamic Financing Platform
version: 1.0.0
appVersion: "1.0.0"
```

#### values.yaml (with environment overrides)
```yaml
global:
  environment: prod
  imageRegistry: registry.ksa-financing.com
  imagePullSecrets:
    - name: registry-credentials

services:
  lendingService:
    enabled: true
    replicas: 3
    image:
      repository: lending-service
      tag: "1.0.0"
    resources:
      requests:
        memory: "512Mi"
        cpu: "500m"
      limits:
        memory: "2Gi"
        cpu: "2000m"

  # Repeat for all 20 services...

infrastructure:
  postgres:
    enabled: true
    storage: 100Gi
  kafka:
    enabled: true
    partitions: 3
  temporal:
    enabled: true
```

---

## 🧪 Deployment

```bash
# Build all Docker images
for service in services/*/; do
  cd $service
  docker build -t ksa-financing/$(basename $service):1.0.0 .
  docker push ksa-financing/$(basename $service):1.0.0
  cd ../..
done

# Deploy using Helm
helm install ksa-financing-platform \
  infrastructure/kubernetes/helm/ksa-financing-platform \
  --namespace ksa-financing-prod \
  --create-namespace \
  --values infrastructure/kubernetes/helm/ksa-financing-platform/values-prod.yaml

# Verify deployment
kubectl get pods -n ksa-financing-prod

# Check logs
kubectl logs -f -n ksa-financing-prod -l app=lending-service

# Port forward for testing
kubectl port-forward -n ksa-financing-prod svc/lending-service 8080:8080
```

---

## ✅ Success Criteria

- [ ] All 20 microservices deployed
- [ ] All infrastructure (Postgres, Kafka, Temporal, Keycloak) running
- [ ] All pods healthy and passing probes
- [ ] HPA scaling working
- [ ] Ingress routing requests correctly
- [ ] TLS certificates issued
- [ ] Services communicating via gRPC
- [ ] Kafka events flowing
- [ ] Temporal workflows executing
- [ ] Logs flowing to ELK
- [ ] Metrics in Prometheus
- [ ] Traces in Jaeger
- [ ] Keycloak authentication working
- [ ] Multi-tenancy isolation enforced
- [ ] Database migrations applied (Flyway)
- [ ] Health checks passing: `curl https://api.ksa-financing.com/actuator/health`

---

## 🎉 PLATFORM COMPLETE!

All 34 prompts executed successfully!

### What You've Built:
✅ 8 SDKs (shared libraries)
✅ 1 Service Template (Hexagonal Architecture)
✅ 20 Microservices (complete platform)
✅ 4 Infrastructure setups (Docker + Kubernetes)

### Total Lines of Code (Estimated):
~91,000 lines across all services and SDKs

### Next Steps:
1. **Performance Testing**: Load test with JMeter/Gatling
2. **Security Audit**: Penetration testing
3. **SAMA Compliance Audit**: Verify regulatory compliance
4. **Go-Live Checklist**: Production readiness review
5. **Training**: Onboard development team
6. **Documentation**: Update runbooks and playbooks

---

## 🚀 Congratulations!

You've successfully implemented the complete **KSA Islamic Financing Platform**! 🎊
