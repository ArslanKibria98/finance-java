# ELK Stack for KSA Islamic Financing Platform

This directory contains Kubernetes configurations for deploying the ELK (Elasticsearch, Logstash, Kibana) stack with Filebeat for centralized logging.

## Overview

The ELK stack provides:
- **Elasticsearch**: Distributed search and analytics engine for storing logs
- **Logstash**: Log processing pipeline for parsing and enriching logs
- **Kibana**: Visualization and exploration UI for logs
- **Filebeat**: Lightweight log shipper running on all nodes

## Architecture

```
┌─────────────┐
│  Filebeat   │ (DaemonSet - runs on every node)
│   Agents    │ Collects logs from /var/log/containers/*.log
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Logstash   │ (2 replicas)
│  Pipeline   │ Parses JSON, adds metadata, routes to indices
└──────┬──────┘
       │
       ▼
┌─────────────┐
│Elasticsearch│ (3 replicas, StatefulSet)
│   Cluster   │ Stores logs with 50GB per node
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Kibana    │ (1 replica)
│   Web UI    │ Visualize and search logs
└─────────────┘
```

## Directory Structure

```
elk/
├── namespace.yaml              # Logging namespace
├── elasticsearch/
│   ├── statefulset.yaml       # 3-node ES cluster with PVCs
│   └── service.yaml           # Headless + client services
├── logstash/
│   ├── configmap.yaml         # Pipeline configuration
│   ├── deployment.yaml        # 2 replicas
│   └── service.yaml           # Internal service
├── kibana/
│   ├── deployment.yaml        # Web UI
│   ├── service.yaml           # ClusterIP service
│   └── ingress.yaml           # External access
├── filebeat/
│   ├── configmap.yaml         # Log collection config
│   └── daemonset.yaml         # Runs on all nodes + RBAC
├── deploy-elk.sh              # Deployment script
├── verify-elk.sh              # Verification script
└── README.md                  # This file
```

## Resource Requirements

| Component      | Replicas | CPU Request | CPU Limit | Memory Request | Memory Limit | Storage |
|----------------|----------|-------------|-----------|----------------|--------------|---------|
| Elasticsearch  | 3        | 1 core      | 2 cores   | 2 GB          | 4 GB         | 50 GB   |
| Logstash       | 2        | 500m        | 1 core    | 512 MB        | 1 GB         | -       |
| Kibana         | 1        | 500m        | 1 core    | 1 GB          | 2 GB         | -       |
| Filebeat       | N nodes  | 100m        | 200m      | 100 MB        | 200 MB       | Host    |

**Total Minimum Requirements:**
- **CPU**: ~5 cores
- **Memory**: ~12 GB
- **Storage**: 150 GB (3 x 50 GB PVCs)

## Deployment

### Quick Start

```bash
# Deploy the entire stack
./deploy-elk.sh

# Verify deployment
./verify-elk.sh
```

### Manual Deployment

```bash
# 1. Create namespace
kubectl apply -f namespace.yaml

# 2. Deploy Elasticsearch (will take 3-5 minutes)
kubectl apply -f elasticsearch/
kubectl wait --for=condition=ready pod -l app=elasticsearch -n logging --timeout=600s

# 3. Deploy Logstash
kubectl apply -f logstash/
kubectl wait --for=condition=ready pod -l app=logstash -n logging --timeout=300s

# 4. Deploy Kibana
kubectl apply -f kibana/
kubectl wait --for=condition=ready pod -l app=kibana -n logging --timeout=300s

# 5. Deploy Filebeat
kubectl apply -f filebeat/
```

### Verify Deployment

```bash
# Check all resources
kubectl get all -n logging

# Check PVCs
kubectl get pvc -n logging

# Check pod status
kubectl get pods -n logging -w

# Run verification script
./verify-elk.sh
```

## Accessing Kibana

### Option 1: Port Forward (Recommended for local development)

```bash
kubectl port-forward -n logging svc/kibana 5601:5601
```

Then open: http://localhost:5601

### Option 2: Ingress (Production)

1. Add to `/etc/hosts`:
   ```
   <cluster-ip> kibana.ksa-financing.local
   ```

2. Access: http://kibana.ksa-financing.local

### Option 3: NodePort (Alternative)

Modify `kibana/service.yaml` to use `type: NodePort` and access via node IP.

## Initial Kibana Setup

1. **Access Kibana** (using one of the methods above)

2. **Create Index Pattern**:
   - Go to **Management** → **Stack Management** → **Index Patterns**
   - Click **Create index pattern**
   - Enter pattern: `ksa-financing-*`
   - Click **Next step**
   - Select time field: `@timestamp`
   - Click **Create index pattern**

3. **View Logs**:
   - Go to **Discover**
   - Select `ksa-financing-*` index pattern
   - Logs should start appearing

## Log Format

Application logs should be in JSON format for optimal parsing:

```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "level": "INFO",
  "logger": "com.ksa.financing.lending.service.LoanService",
  "message": "Loan application submitted",
  "tenant_id": "tenant-abc",
  "user_id": "user-123",
  "loan_id": "loan-456",
  "trace_id": "abc123def456",
  "span_id": "789xyz"
}
```

### Log Indices

Logs are automatically routed to daily indices:
- With tenant: `ksa-financing-tenant-{tenant_id}-{YYYY.MM.dd}`
- Without tenant: `ksa-financing-{YYYY.MM.dd}`

## Configuration

### Elasticsearch Configuration

Key settings in `elasticsearch/statefulset.yaml`:
- **Cluster name**: `ksa-financing-logs`
- **Discovery**: StatefulSet-based with 3 nodes
- **Java heap**: 2GB (adjustable via `ES_JAVA_OPTS`)
- **Security**: Disabled for simplicity (enable in production)

### Logstash Pipeline

The pipeline in `logstash/configmap.yaml`:
1. **Input**: Receives logs from Filebeat on port 5044
2. **Filter**:
   - Parses JSON logs
   - Adds Kubernetes metadata
   - Extracts tenant ID for multi-tenant routing
   - Normalizes timestamps
3. **Output**: Sends to Elasticsearch with dynamic index names

### Filebeat Configuration

Filebeat in `filebeat/configmap.yaml`:
- Collects container logs from `/var/log/containers/*.log`
- Adds Kubernetes metadata (namespace, pod, labels)
- Excludes system namespaces (`kube-system`, etc.)
- Sends to Logstash for processing

## Monitoring

### Check Elasticsearch Health

```bash
# Port forward Elasticsearch
kubectl port-forward -n logging svc/elasticsearch-client 9200:9200

# Check cluster health
curl http://localhost:9200/_cluster/health?pretty

# Check nodes
curl http://localhost:9200/_cat/nodes?v

# Check indices
curl http://localhost:9200/_cat/indices?v
```

### Check Logs

```bash
# Elasticsearch logs
kubectl logs -n logging -l app=elasticsearch --tail=50

# Logstash logs
kubectl logs -n logging -l app=logstash --tail=50

# Kibana logs
kubectl logs -n logging -l app=kibana --tail=50

# Filebeat logs
kubectl logs -n logging -l app=filebeat --tail=50
```

### Check Resource Usage

```bash
# CPU and Memory usage
kubectl top pods -n logging

# Storage usage
kubectl get pvc -n logging
```

## Troubleshooting

### Elasticsearch Pods Not Starting

1. **Check events**:
   ```bash
   kubectl get events -n logging --sort-by='.lastTimestamp'
   ```

2. **Common issues**:
   - Insufficient memory: Reduce `ES_JAVA_OPTS` or increase node memory
   - PVC not bound: Check storage class exists
   - Init container failed: May need privileged security context

3. **Check logs**:
   ```bash
   kubectl logs -n logging elasticsearch-0 --previous
   ```

### Logstash Not Processing Logs

1. **Verify connection to Elasticsearch**:
   ```bash
   kubectl logs -n logging -l app=logstash | grep -i elasticsearch
   ```

2. **Check Logstash pipeline**:
   ```bash
   kubectl exec -n logging -it deployment/logstash -- cat /usr/share/logstash/pipeline/logstash.conf
   ```

### Kibana Not Accessible

1. **Check pod status**:
   ```bash
   kubectl get pods -n logging -l app=kibana
   ```

2. **Check connection to Elasticsearch**:
   ```bash
   kubectl logs -n logging -l app=kibana | grep -i elasticsearch
   ```

### No Logs Appearing in Kibana

1. **Check Filebeat is collecting logs**:
   ```bash
   kubectl logs -n logging -l app=filebeat | grep -i "publish"
   ```

2. **Verify logs are reaching Logstash**:
   ```bash
   kubectl logs -n logging -l app=logstash | tail -20
   ```

3. **Check Elasticsearch indices**:
   ```bash
   kubectl port-forward -n logging svc/elasticsearch-client 9200:9200
   curl http://localhost:9200/_cat/indices?v
   ```

## Scaling

### Scale Logstash

```bash
kubectl scale deployment logstash -n logging --replicas=3
```

### Scale Elasticsearch (Advanced)

Scaling Elasticsearch requires careful consideration:

```bash
# Scale up
kubectl scale statefulset elasticsearch -n logging --replicas=5

# Wait for new nodes to join cluster
kubectl logs -n logging elasticsearch-3 | grep "successfully joined"

# Verify cluster health
kubectl port-forward -n logging svc/elasticsearch-client 9200:9200
curl http://localhost:9200/_cluster/health?pretty
```

## Backup and Restore

### Snapshot Repository Setup

```bash
# Create snapshot repository (using MinIO/S3)
curl -X PUT "localhost:9200/_snapshot/backup_repo?pretty" -H 'Content-Type: application/json' -d'
{
  "type": "s3",
  "settings": {
    "bucket": "elk-backups",
    "endpoint": "minio.ksa-financing.local"
  }
}
'

# Create snapshot
curl -X PUT "localhost:9200/_snapshot/backup_repo/snapshot_1?wait_for_completion=true"

# Restore snapshot
curl -X POST "localhost:9200/_snapshot/backup_repo/snapshot_1/_restore"
```

## Security Considerations

### For Production Deployment

1. **Enable Elasticsearch Security**:
   - Set `xpack.security.enabled: true`
   - Configure TLS for node-to-node communication
   - Set up user authentication

2. **Secure Kibana**:
   - Enable TLS on Ingress
   - Integrate with Keycloak for SSO
   - Configure RBAC for users

3. **Network Policies**:
   - Restrict pod-to-pod communication
   - Only allow Filebeat → Logstash → Elasticsearch

4. **Secrets Management**:
   - Store credentials in Kubernetes Secrets
   - Use HashiCorp Vault for key management

## Log Retention

Default retention in `logstash/configmap.yaml`:
- **Index pattern**: Daily rotation
- **Retention**: Configure Index Lifecycle Management (ILM)

### Example ILM Policy

```json
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "1d"
          }
        }
      },
      "delete": {
        "min_age": "30d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

Apply via Kibana UI or API.

## Integration with Technology Stack

This ELK stack aligns with the [Technology Stack Blueprint](../../../docs/islamic-financing/master-blueprint/02_TECHNOLOGY_STACK.md):

- **Section 8.2**: Loki + Promtail → **ELK Alternative** (more mature, better for structured logs)
- **Structured Logging**: JSON format with correlation IDs
- **Retention**: 30 days hot, configurable for compliance (7 years for audit logs)
- **Integration**: Works with Istio tracing, Prometheus metrics

## Next Steps

After successful ELK deployment:

1. ✅ **Verify logs are flowing** using Kibana Discover
2. 📊 **Create dashboards** for key metrics (errors, latency, etc.)
3. 🚨 **Set up alerts** using Elasticsearch Watcher or external tools
4. 🔄 **Proceed to Prompt 03**: Kubernetes Monitoring Stack (Prometheus, Grafana, Jaeger)

## References

- [Elasticsearch Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Logstash Documentation](https://www.elastic.co/guide/en/logstash/current/index.html)
- [Kibana Documentation](https://www.elastic.co/guide/en/kibana/current/index.html)
- [Filebeat Documentation](https://www.elastic.co/guide/en/beats/filebeat/current/index.html)
- [Running Elastic Stack on Kubernetes](https://www.elastic.co/guide/en/cloud-on-k8s/current/index.html)

## Support

For issues or questions:
1. Check logs: `kubectl logs -n logging <pod-name>`
2. Run verification: `./verify-elk.sh`
3. Review troubleshooting section above
4. Check Kubernetes events: `kubectl get events -n logging`
