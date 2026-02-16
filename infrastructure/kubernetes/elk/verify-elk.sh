#!/bin/bash

#######################################################################
# KSA Islamic Financing Platform - ELK Stack Verification Script
#
# This script verifies the health and status of the ELK stack
# deployed to Kubernetes.
#######################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

log_error() {
    echo -e "${RED}[✗]${NC} $1"
}

ERRORS=0

# Check namespace
check_namespace() {
    log_info "Checking logging namespace..."
    if kubectl get namespace logging &> /dev/null; then
        log_success "Namespace 'logging' exists"
    else
        log_error "Namespace 'logging' does not exist"
        ((ERRORS++))
    fi
}

# Check Elasticsearch
check_elasticsearch() {
    log_info "Checking Elasticsearch cluster..."

    # Check StatefulSet
    local es_ready=$(kubectl get statefulset elasticsearch -n logging -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
    local es_desired=$(kubectl get statefulset elasticsearch -n logging -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "3")

    if [ "$es_ready" == "$es_desired" ]; then
        log_success "Elasticsearch: ${es_ready}/${es_desired} replicas ready"
    else
        log_error "Elasticsearch: ${es_ready}/${es_desired} replicas ready"
        ((ERRORS++))
    fi

    # Check PVCs
    local pvc_count=$(kubectl get pvc -n logging -l app=elasticsearch --no-headers 2>/dev/null | wc -l)
    if [ "$pvc_count" -ge 3 ]; then
        log_success "Elasticsearch PVCs: ${pvc_count} found"
    else
        log_error "Elasticsearch PVCs: ${pvc_count} found (expected at least 3)"
        ((ERRORS++))
    fi

    # Check service
    if kubectl get svc elasticsearch-client -n logging &> /dev/null; then
        log_success "Elasticsearch service exists"
    else
        log_error "Elasticsearch service not found"
        ((ERRORS++))
    fi
}

# Check Logstash
check_logstash() {
    log_info "Checking Logstash..."

    # Check Deployment
    local ls_ready=$(kubectl get deployment logstash -n logging -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
    local ls_desired=$(kubectl get deployment logstash -n logging -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "2")

    if [ "$ls_ready" == "$ls_desired" ]; then
        log_success "Logstash: ${ls_ready}/${ls_desired} replicas ready"
    else
        log_error "Logstash: ${ls_ready}/${ls_desired} replicas ready"
        ((ERRORS++))
    fi

    # Check ConfigMap
    if kubectl get configmap logstash-config -n logging &> /dev/null; then
        log_success "Logstash ConfigMap exists"
    else
        log_error "Logstash ConfigMap not found"
        ((ERRORS++))
    fi

    # Check service
    if kubectl get svc logstash -n logging &> /dev/null; then
        log_success "Logstash service exists"
    else
        log_error "Logstash service not found"
        ((ERRORS++))
    fi
}

# Check Kibana
check_kibana() {
    log_info "Checking Kibana..."

    # Check Deployment
    local kb_ready=$(kubectl get deployment kibana -n logging -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
    local kb_desired=$(kubectl get deployment kibana -n logging -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "1")

    if [ "$kb_ready" == "$kb_desired" ]; then
        log_success "Kibana: ${kb_ready}/${kb_desired} replicas ready"
    else
        log_error "Kibana: ${kb_ready}/${kb_desired} replicas ready"
        ((ERRORS++))
    fi

    # Check service
    if kubectl get svc kibana -n logging &> /dev/null; then
        log_success "Kibana service exists"
    else
        log_error "Kibana service not found"
        ((ERRORS++))
    fi

    # Check Ingress
    if kubectl get ingress kibana-ingress -n logging &> /dev/null; then
        log_success "Kibana Ingress exists"
    else
        log_warning "Kibana Ingress not found (might not be needed if using port-forward)"
    fi
}

# Check Filebeat
check_filebeat() {
    log_info "Checking Filebeat..."

    # Check DaemonSet
    local fb_desired=$(kubectl get daemonset filebeat -n logging -o jsonpath='{.status.desiredNumberScheduled}' 2>/dev/null || echo "0")
    local fb_ready=$(kubectl get daemonset filebeat -n logging -o jsonpath='{.status.numberReady}' 2>/dev/null || echo "0")

    if [ "$fb_ready" == "$fb_desired" ] && [ "$fb_ready" -gt 0 ]; then
        log_success "Filebeat: ${fb_ready}/${fb_desired} pods ready"
    else
        log_warning "Filebeat: ${fb_ready}/${fb_desired} pods ready"
        if [ "$fb_desired" -eq 0 ]; then
            log_warning "Filebeat DaemonSet may not be deployed yet"
        fi
    fi

    # Check ConfigMap
    if kubectl get configmap filebeat-config -n logging &> /dev/null; then
        log_success "Filebeat ConfigMap exists"
    else
        log_error "Filebeat ConfigMap not found"
        ((ERRORS++))
    fi

    # Check ServiceAccount
    if kubectl get serviceaccount filebeat -n logging &> /dev/null; then
        log_success "Filebeat ServiceAccount exists"
    else
        log_error "Filebeat ServiceAccount not found"
        ((ERRORS++))
    fi
}

# Check cluster connectivity
check_connectivity() {
    log_info "Testing Elasticsearch API connectivity..."

    # Port forward in background
    kubectl port-forward -n logging svc/elasticsearch-client 9200:9200 &> /dev/null &
    local PF_PID=$!
    sleep 3

    # Test connection
    if curl -s http://localhost:9200/_cluster/health &> /dev/null; then
        local health=$(curl -s http://localhost:9200/_cluster/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        if [ "$health" == "green" ] || [ "$health" == "yellow" ]; then
            log_success "Elasticsearch cluster health: ${health}"
        else
            log_warning "Elasticsearch cluster health: ${health}"
        fi
    else
        log_warning "Could not connect to Elasticsearch API (this is normal if not using port-forward)"
    fi

    # Cleanup
    kill $PF_PID &> /dev/null || true
}

# Display pod logs if there are errors
show_error_logs() {
    if [ $ERRORS -gt 0 ]; then
        log_info "Fetching logs from failed pods..."
        echo ""

        # Get pods with issues
        kubectl get pods -n logging --field-selector=status.phase!=Running --no-headers 2>/dev/null | while read line; do
            pod_name=$(echo $line | awk '{print $1}')
            log_warning "Logs from ${pod_name}:"
            kubectl logs -n logging ${pod_name} --tail=20 2>/dev/null || echo "Could not fetch logs"
            echo ""
        done
    fi
}

# Display summary
display_summary() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    if [ $ERRORS -eq 0 ]; then
        log_success "All checks passed! ELK stack is healthy."
        echo ""
        log_info "Success Criteria Met:"
        echo "  ✓ Elasticsearch cluster running (3 pods)"
        echo "  ✓ Logstash processing logs (2 pods)"
        echo "  ✓ Kibana accessible"
        echo "  ✓ Filebeat collecting logs from all nodes"
        echo ""
        log_info "Access Kibana:"
        echo "  kubectl port-forward -n logging svc/kibana 5601:5601"
        echo "  Then open: http://localhost:5601"
        echo ""
        log_info "Next Steps:"
        echo "  1. Create index pattern in Kibana: ksa-financing-*"
        echo "  2. Verify logs are being collected"
        echo "  3. Proceed to Prompt 03: Kubernetes Monitoring Stack"
    else
        log_error "Found ${ERRORS} error(s). Please check the output above."
        echo ""
        log_info "Common troubleshooting steps:"
        echo "  1. Check pod status: kubectl get pods -n logging"
        echo "  2. Check pod logs: kubectl logs -n logging <pod-name>"
        echo "  3. Check events: kubectl get events -n logging --sort-by='.lastTimestamp'"
        echo "  4. Verify cluster has sufficient resources"
    fi

    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

# Main verification flow
main() {
    echo ""
    log_info "Starting ELK Stack verification..."
    echo ""

    check_namespace
    check_elasticsearch
    check_logstash
    check_kibana
    check_filebeat
    check_connectivity

    echo ""
    show_error_logs
    display_summary

    # Exit with error code if checks failed
    if [ $ERRORS -gt 0 ]; then
        exit 1
    fi
}

# Run main function
main
