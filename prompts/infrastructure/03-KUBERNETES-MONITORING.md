# 📊 Infrastructure Prompt 03: Kubernetes Monitoring Stack

**Objective**: Deploy Prometheus, Grafana, and Jaeger to Kubernetes for metrics and tracing.

**Prerequisites**: ✅ ELK Stack deployed (Prompt 02)

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/09_OBSERVABILITY_OPERATIONS.md` - Monitoring architecture
- `/var/www/docs/islamic-financing/master-blueprint/02_TECHNOLOGY_STACK.md` - Versions

---

## 🎯 Implementation Requirements

### Directory Structure
```
infrastructure/kubernetes/monitoring/
├── namespace.yaml
├── prometheus/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── configmap.yaml
│   └── servicemonitor-crd.yaml
├── grafana/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── ingress.yaml
│   └── dashboards/
│       ├── jvm-dashboard.json
│       ├── temporal-dashboard.json
│       └── business-metrics-dashboard.json
└── jaeger/
    ├── deployment.yaml
    ├── service.yaml
    └── ingress.yaml
```

### What to Create

#### 1. Prometheus Setup
- Deployment with persistent storage
- ServiceMonitor CRDs to scrape services
- Scrape all Spring Boot Actuator endpoints
- Retention: 15 days

#### 2. Grafana Setup
- Pre-configured Prometheus datasource
- Import dashboards:
  - **JVM Metrics**: Heap, GC, threads
  - **Temporal Metrics**: Workflow/activity stats
  - **Business Metrics**: Loans, disbursements, repayments
  - **Infrastructure Metrics**: CPU, memory, disk

#### 3. Jaeger Setup
- All-in-one deployment (for non-prod)
- Collector, Query, UI components
- Storage: Elasticsearch (reuse from ELK)

### Prometheus ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: monitoring
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s

    scrape_configs:
      # Scrape all services with prometheus.io/scrape annotation
      - job_name: 'kubernetes-pods'
        kubernetes_sd_configs:
          - role: pod
        relabel_configs:
          - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
            action: keep
            regex: true
          - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
            action: replace
            target_label: __metrics_path__
            regex: (.+)
          - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
            action: replace
            regex: ([^:]+)(?::\d+)?;(\d+)
            replacement: $1:$2
            target_label: __address__
```

### Grafana Dashboards

#### JVM Dashboard (jvm-dashboard.json)
Metrics to include:
- `jvm_memory_used_bytes`
- `jvm_gc_pause_seconds`
- `jvm_threads_live`
- `process_cpu_usage`

#### Business Metrics Dashboard (business-metrics-dashboard.json)
Metrics to include:
- `loans_created_total`
- `loans_approved_total`
- `loans_disbursed_total`
- `repayments_received_total`
- `loan_amount_disbursed_sar`

### Service Annotations

Services must be annotated for Prometheus scraping:
```yaml
metadata:
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/port: "8080"
    prometheus.io/path: "/actuator/prometheus"
```

---

## 🧪 Deployment

```bash
# Create namespace
kubectl apply -f infrastructure/kubernetes/monitoring/namespace.yaml

# Deploy Prometheus
kubectl apply -f infrastructure/kubernetes/monitoring/prometheus/

# Deploy Grafana
kubectl apply -f infrastructure/kubernetes/monitoring/grafana/

# Deploy Jaeger
kubectl apply -f infrastructure/kubernetes/monitoring/jaeger/

# Verify
kubectl get pods -n monitoring

# Access UIs
kubectl port-forward -n monitoring svc/grafana 3000:3000
kubectl port-forward -n monitoring svc/jaeger-query 16686:16686
```

---

## ✅ Success Criteria

- [ ] Prometheus scraping all services
- [ ] Grafana accessible via Ingress
- [ ] All dashboards imported and working
- [ ] Jaeger receiving traces from services
- [ ] Service mesh (if used) integrated
- [ ] Alerts configured in Prometheus
- [ ] Alert routing to Slack/email

---

## 🔄 Next Step

After monitoring setup, proceed to:
- **Prompt 04**: Kubernetes Service Deployments (Final)
