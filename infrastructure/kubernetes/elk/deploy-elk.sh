#!/bin/bash

#######################################################################
# KSA Islamic Financing Platform - ELK Stack Deployment Script
#
# This script deploys the ELK (Elasticsearch, Logstash, Kibana) stack
# to Kubernetes for centralized logging.
#
# Prerequisites:
# - kubectl configured and connected to cluster
# - Sufficient cluster resources (CPU, Memory, Storage)
#######################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Base directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed or not in PATH"
        exit 1
    fi

    if ! kubectl cluster-info &> /dev/null; then
        log_error "kubectl is not connected to a cluster"
        exit 1
    fi

    log_success "Prerequisites check passed"
}

# Create namespace
deploy_namespace() {
    log_info "Creating logging namespace..."
    kubectl apply -f "${SCRIPT_DIR}/namespace.yaml"
    log_success "Namespace created"
}

# Deploy Elasticsearch
deploy_elasticsearch() {
    log_info "Deploying Elasticsearch cluster (3 replicas)..."
    kubectl apply -f "${SCRIPT_DIR}/elasticsearch/"

    log_info "Waiting for Elasticsearch to be ready (this may take 3-5 minutes)..."
    kubectl wait --for=condition=ready pod -l app=elasticsearch -n logging --timeout=600s || {
        log_error "Elasticsearch failed to become ready"
        log_info "Checking pod status:"
        kubectl get pods -n logging -l app=elasticsearch
        log_info "Checking pod logs:"
        kubectl logs -n logging -l app=elasticsearch --tail=50
        exit 1
    }

    log_success "Elasticsearch cluster is ready"
}

# Deploy Logstash
deploy_logstash() {
    log_info "Deploying Logstash (2 replicas)..."
    kubectl apply -f "${SCRIPT_DIR}/logstash/"

    log_info "Waiting for Logstash to be ready..."
    kubectl wait --for=condition=ready pod -l app=logstash -n logging --timeout=300s || {
        log_error "Logstash failed to become ready"
        log_info "Checking pod status:"
        kubectl get pods -n logging -l app=logstash
        exit 1
    }

    log_success "Logstash is ready"
}

# Deploy Kibana
deploy_kibana() {
    log_info "Deploying Kibana..."
    kubectl apply -f "${SCRIPT_DIR}/kibana/"

    log_info "Waiting for Kibana to be ready..."
    kubectl wait --for=condition=ready pod -l app=kibana -n logging --timeout=300s || {
        log_error "Kibana failed to become ready"
        log_info "Checking pod status:"
        kubectl get pods -n logging -l app=kibana
        exit 1
    }

    log_success "Kibana is ready"
}

# Deploy Filebeat
deploy_filebeat() {
    log_info "Deploying Filebeat DaemonSet..."
    kubectl apply -f "${SCRIPT_DIR}/filebeat/"

    log_info "Waiting for Filebeat to be ready on all nodes..."
    sleep 10

    DESIRED=$(kubectl get daemonset filebeat -n logging -o jsonpath='{.status.desiredNumberScheduled}')
    READY=$(kubectl get daemonset filebeat -n logging -o jsonpath='{.status.numberReady}')

    log_info "Filebeat: ${READY}/${DESIRED} pods ready"

    if [ "$READY" != "$DESIRED" ]; then
        log_warning "Not all Filebeat pods are ready. This may be expected if nodes are being scheduled."
        log_info "Checking DaemonSet status:"
        kubectl get daemonset filebeat -n logging
    fi

    log_success "Filebeat DaemonSet deployed"
}

# Display deployment status
display_status() {
    log_info "Deployment Status:"
    echo ""
    kubectl get all -n logging
    echo ""

    log_info "Persistent Volume Claims:"
    kubectl get pvc -n logging
    echo ""

    log_info "Ingress:"
    kubectl get ingress -n logging
    echo ""
}

# Display access information
display_access_info() {
    log_success "ELK Stack deployment completed successfully!"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    log_info "Access Information:"
    echo ""
    echo "  Kibana Web UI:"
    echo "    - URL: http://kibana.ksa-financing.local:5601"
    echo "    - Add 'kibana.ksa-financing.local' to /etc/hosts pointing to cluster IP"
    echo ""
    echo "  Port Forwarding (alternative):"
    echo "    kubectl port-forward -n logging svc/kibana 5601:5601"
    echo "    Then access: http://localhost:5601"
    echo ""
    echo "  Elasticsearch API:"
    echo "    kubectl port-forward -n logging svc/elasticsearch-client 9200:9200"
    echo "    Then access: http://localhost:9200"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    log_info "Next Steps:"
    echo "  1. Access Kibana and create index pattern: ksa-financing-*"
    echo "  2. Deploy your application services that will generate logs"
    echo "  3. Configure alerts and dashboards in Kibana"
    echo "  4. Proceed to Prompt 03: Kubernetes Monitoring Stack"
    echo ""
}

# Main deployment flow
main() {
    log_info "Starting ELK Stack deployment to Kubernetes..."
    echo ""

    check_prerequisites
    deploy_namespace
    deploy_elasticsearch
    deploy_logstash
    deploy_kibana
    deploy_filebeat

    echo ""
    display_status
    display_access_info
}

# Run main function
main
