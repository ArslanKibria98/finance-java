# ☸️ Infrastructure Prompt 02: Kubernetes ELK Stack

**Objective**: Deploy ELK Stack (Elasticsearch, Logstash, Kibana, Filebeat) to Kubernetes for centralized logging.

**Prerequisites**: ✅ Docker Compose setup (Prompt 01)

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/09_OBSERVABILITY_OPERATIONS.md` - ELK configuration
- `/var/www/docs/islamic-financing/master-blueprint/02_TECHNOLOGY_STACK.md` - Versions

---

## 🎯 Implementation Requirements

### Directory Structure
```
infrastructure/kubernetes/elk/
├── namespace.yaml
├── elasticsearch/
│   ├── statefulset.yaml
│   ├── service.yaml
│   └── pvc.yaml
├── logstash/
│   ├── deployment.yaml
│   ├── service.yaml
│   └── configmap.yaml
├── kibana/
│   ├── deployment.yaml
│   └── service.yaml
└── filebeat/
    ├── daemonset.yaml
    └── configmap.yaml
```

### What to Create

#### 1. Namespace
```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: logging
```

#### 2. Elasticsearch StatefulSet
- 3 replicas for high availability
- Persistent volumes (50GB each)
- Memory: 4GB per pod
- CPU: 2 cores per pod

#### 3. Logstash Deployment
- 2 replicas
- Parses JSON logs from services
- Sends to Elasticsearch

#### 4. Kibana Deployment
- 1 replica
- Ingress for external access
- Pre-configured dashboards

#### 5. Filebeat DaemonSet
- Runs on every node
- Collects logs from all pods
- Sends to Logstash

### Configuration Files

#### Logstash ConfigMap
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: logstash-config
  namespace: logging
data:
  logstash.conf: |
    input {
      beats {
        port => 5044
      }
    }
    filter {
      json {
        source => "message"
      }
      mutate {
        add_field => { "[@metadata][target_index]" => "ksa-financing-%{+YYYY.MM.dd}" }
      }
    }
    output {
      elasticsearch {
        hosts => ["elasticsearch:9200"]
        index => "%{[@metadata][target_index]}"
      }
    }
```

#### Filebeat ConfigMap
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: filebeat-config
  namespace: logging
data:
  filebeat.yml: |
    filebeat.inputs:
    - type: container
      paths:
        - /var/log/containers/*.log
      processors:
        - add_kubernetes_metadata:
            host: ${NODE_NAME}
            matchers:
            - logs_path:
                logs_path: "/var/log/containers/"

    output.logstash:
      hosts: ["logstash:5044"]
```

### Ingress

Create Ingress for Kibana:
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: kibana-ingress
  namespace: logging
spec:
  rules:
  - host: kibana.ksa-financing.local
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: kibana
            port:
              number: 5601
```

---

## 🧪 Deployment

```bash
# Create namespace
kubectl apply -f infrastructure/kubernetes/elk/namespace.yaml

# Deploy Elasticsearch
kubectl apply -f infrastructure/kubernetes/elk/elasticsearch/

# Wait for Elasticsearch to be ready
kubectl wait --for=condition=ready pod -l app=elasticsearch -n logging --timeout=300s

# Deploy Logstash
kubectl apply -f infrastructure/kubernetes/elk/logstash/

# Deploy Kibana
kubectl apply -f infrastructure/kubernetes/elk/kibana/

# Deploy Filebeat
kubectl apply -f infrastructure/kubernetes/elk/filebeat/

# Verify
kubectl get pods -n logging
```

---

## ✅ Success Criteria

- [ ] Elasticsearch cluster running (3 pods)
- [ ] Logstash processing logs (2 pods)
- [ ] Kibana accessible via Ingress
- [ ] Filebeat collecting logs from all nodes
- [ ] Logs visible in Kibana
- [ ] Index pattern created: `ksa-financing-*`

---

## 🔄 Next Step

After ELK deployment, proceed to:
- **Prompt 03**: Kubernetes Monitoring Stack (Prometheus, Grafana, Jaeger)
