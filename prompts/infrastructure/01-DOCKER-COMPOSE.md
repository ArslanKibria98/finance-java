# 🐳 Infrastructure Prompt 01: Docker Compose Local Development

**Objective**: Create Docker Compose setup for local development with all infrastructure services.

**Prerequisites**: ✅ All SDK prompts + Service template complete

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/02_TECHNOLOGY_STACK.md` - All technology versions
- `/var/www/docs/islamic-financing/master-blueprint/09_OBSERVABILITY_OPERATIONS.md` - Monitoring stack

---

## 🎯 Implementation Requirements

### What to Create

Create `/var/www/islamic-financing-platform/docker-compose.yml` with:

### Infrastructure Services

#### 1. PostgreSQL (18.1)
```yaml
postgres:
  image: postgres:18.1
  environment:
    POSTGRES_PASSWORD: postgres
    POSTGRES_DB: ksa_financing
  ports:
    - "5432:5432"
  volumes:
    - postgres_data:/var/lib/postgresql/data
```

#### 2. Redis (8.0.2)
```yaml
redis:
  image: redis:8.0.2
  ports:
    - "6379:6379"
```

#### 3. Kafka (3.9.1) + Zookeeper + Schema Registry
```yaml
zookeeper:
  image: confluentinc/cp-zookeeper:7.8.0

kafka:
  image: confluentinc/cp-kafka:7.8.0
  depends_on:
    - zookeeper
  ports:
    - "9092:9092"

schema-registry:
  image: confluentinc/cp-schema-registry:7.8.0
  depends_on:
    - kafka
  ports:
    - "8081:8081"
```

#### 4. Temporal (Latest)
```yaml
temporal:
  image: temporalio/auto-setup:latest
  ports:
    - "7233:7233"
    - "8233:8233"  # UI
  depends_on:
    - postgres
```

#### 5. Keycloak (26.5.2)
```yaml
keycloak:
  image: quay.io/keycloak/keycloak:26.5.2
  command: start-dev
  environment:
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin
  ports:
    - "8080:8080"
```

#### 6. Elasticsearch (9.3.0)
```yaml
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:9.3.0
  environment:
    - discovery.type=single-node
  ports:
    - "9200:9200"
```

#### 7. Logstash (9.3.0)
```yaml
logstash:
  image: docker.elastic.co/logstash/logstash:9.3.0
  ports:
    - "5000:5000"
  volumes:
    - ./infrastructure/logstash/pipeline:/usr/share/logstash/pipeline
```

#### 8. Kibana (9.3.0)
```yaml
kibana:
  image: docker.elastic.co/kibana/kibana:9.3.0
  ports:
    - "5601:5601"
  depends_on:
    - elasticsearch
```

#### 9. Prometheus (Latest)
```yaml
prometheus:
  image: prom/prometheus:latest
  ports:
    - "9090:9090"
  volumes:
    - ./infrastructure/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
```

#### 10. Grafana (Latest)
```yaml
grafana:
  image: grafana/grafana:latest
  ports:
    - "3000:3000"
  depends_on:
    - prometheus
```

#### 11. Jaeger (Latest)
```yaml
jaeger:
  image: jaegertracing/all-in-one:latest
  ports:
    - "16686:16686"  # UI
    - "14268:14268"  # Collector
```

#### 12. Apache Fineract (1.13.0)
```yaml
fineract:
  image: apache/fineract:1.13.0
  ports:
    - "8443:8443"
  depends_on:
    - postgres
```

### Create Supporting Files

#### `/infrastructure/logstash/pipeline/logstash.conf`
```conf
input {
  tcp {
    port => 5000
    codec => json
  }
}

filter {
  # Parse JSON logs
  json {
    source => "message"
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "ksa-financing-logs-%{+YYYY.MM.dd}"
  }
}
```

#### `/infrastructure/prometheus/prometheus.yml`
```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-boot-services'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
        - 'lending-service:8080'
        - 'customer-service:8080'
        # Add all services
```

### Create Helper Scripts

#### `scripts/start-infra.sh`
```bash
#!/bin/bash
docker-compose up -d postgres redis kafka temporal keycloak elasticsearch logstash kibana prometheus grafana jaeger
```

#### `scripts/stop-infra.sh`
```bash
#!/bin/bash
docker-compose down
```

#### `scripts/logs.sh`
```bash
#!/bin/bash
docker-compose logs -f $1
```

---

## 🧪 Testing

```bash
# Start all infrastructure
./scripts/start-infra.sh

# Verify all services are running
docker-compose ps

# Access UIs
open http://localhost:8080   # Keycloak
open http://localhost:5601   # Kibana
open http://localhost:3000   # Grafana
open http://localhost:8233   # Temporal UI
open http://localhost:16686  # Jaeger UI
```

---

## ✅ Success Criteria

- [ ] All infrastructure services start successfully
- [ ] No port conflicts
- [ ] Kibana connects to Elasticsearch
- [ ] Grafana connects to Prometheus
- [ ] Temporal UI accessible
- [ ] Keycloak admin console accessible
- [ ] All services healthy: `docker-compose ps`

---

## 🔄 Next Step

After local dev setup, proceed to:
- **Prompt 02**: Kubernetes ELK Stack deployment
