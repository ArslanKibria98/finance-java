#!/bin/bash

# Service Template Validation Script
# This script validates that the service template is correctly configured

echo "========================================="
echo "Service Template Validation"
echo "========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Validation results
ERRORS=0
WARNINGS=0

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
        ((ERRORS++))
    fi
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
    ((WARNINGS++))
}

echo ""
echo "1. Checking directory structure..."
echo "-----------------------------------"

# Check essential directories
DIRS=(
    "src/main/java/com/ksa/financing/service/template/domain"
    "src/main/java/com/ksa/financing/service/template/application"
    "src/main/java/com/ksa/financing/service/template/infrastructure"
    "src/main/java/com/ksa/financing/service/template/adapter"
    "src/main/resources/db/migration"
    "src/test/java"
    "k8s"
)

for dir in "${DIRS[@]}"; do
    if [ -d "$dir" ]; then
        print_status 0 "Directory exists: $dir"
    else
        print_status 1 "Missing directory: $dir"
    fi
done

echo ""
echo "2. Checking essential files..."
echo "-------------------------------"

# Check essential files
FILES=(
    "pom.xml"
    "Dockerfile"
    "README.md"
    "src/main/resources/application.yml"
    "src/main/resources/application-dev.yml"
    "src/main/resources/application-prod.yml"
    "k8s/deployment.yaml"
    "k8s/service.yaml"
    "k8s/configmap.yaml"
)

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        print_status 0 "File exists: $file"
    else
        print_status 1 "Missing file: $file"
    fi
done

echo ""
echo "3. Checking Java package structure..."
echo "--------------------------------------"

# Check domain layer components
if [ -f "src/main/java/com/ksa/financing/service/template/domain/model/ExampleAggregate.java" ]; then
    print_status 0 "Domain aggregate found"
else
    print_status 1 "Domain aggregate missing"
fi

if [ -f "src/main/java/com/ksa/financing/service/template/domain/port/in/ManageExampleUseCase.java" ]; then
    print_status 0 "Input port found"
else
    print_status 1 "Input port missing"
fi

if [ -f "src/main/java/com/ksa/financing/service/template/domain/port/out/ExampleRepository.java" ]; then
    print_status 0 "Output port found"
else
    print_status 1 "Output port missing"
fi

# Check application layer
if [ -f "src/main/java/com/ksa/financing/service/template/application/usecase/ManageExampleUseCaseImpl.java" ]; then
    print_status 0 "Use case implementation found"
else
    print_status 1 "Use case implementation missing"
fi

# Check infrastructure layer
if [ -f "src/main/java/com/ksa/financing/service/template/infrastructure/persistence/repository/ExampleRepositoryImpl.java" ]; then
    print_status 0 "Repository implementation found"
else
    print_status 1 "Repository implementation missing"
fi

# Check adapter layer
if [ -f "src/main/java/com/ksa/financing/service/template/adapter/rest/controller/ExampleController.java" ]; then
    print_status 0 "REST controller found"
else
    print_status 1 "REST controller missing"
fi

echo ""
echo "4. Checking Maven configuration..."
echo "-----------------------------------"

# Check if Maven is available
if command -v mvn &> /dev/null; then
    print_status 0 "Maven is installed"

    # Check if project compiles
    echo "Attempting to compile project..."
    mvn clean compile -q 2>/dev/null
    if [ $? -eq 0 ]; then
        print_status 0 "Project compiles successfully"
    else
        print_warning "Project compilation has issues (dependencies might need to be downloaded)"
    fi
else
    print_status 1 "Maven is not installed"
fi

echo ""
echo "5. Checking Docker configuration..."
echo "------------------------------------"

# Check Dockerfile syntax
if [ -f "Dockerfile" ]; then
    # Check for multi-stage build
    if grep -q "FROM.*AS builder" Dockerfile; then
        print_status 0 "Multi-stage Docker build configured"
    else
        print_warning "Single-stage Docker build (consider multi-stage for optimization)"
    fi

    # Check for non-root user
    if grep -q "USER.*appuser" Dockerfile; then
        print_status 0 "Non-root user configured"
    else
        print_warning "Running as root user (security concern)"
    fi
fi

echo ""
echo "6. Checking Kubernetes manifests..."
echo "------------------------------------"

# Check for essential Kubernetes resources
if [ -f "k8s/deployment.yaml" ]; then
    # Check for resource limits
    if grep -q "resources:" k8s/deployment.yaml; then
        print_status 0 "Resource limits configured"
    else
        print_warning "No resource limits in deployment"
    fi

    # Check for probes
    if grep -q "livenessProbe:" k8s/deployment.yaml; then
        print_status 0 "Liveness probe configured"
    else
        print_warning "No liveness probe configured"
    fi

    if grep -q "readinessProbe:" k8s/deployment.yaml; then
        print_status 0 "Readiness probe configured"
    else
        print_warning "No readiness probe configured"
    fi
fi

echo ""
echo "7. Checking test coverage..."
echo "-----------------------------"

# Count test files
TEST_COUNT=$(find src/test -name "*Test.java" 2>/dev/null | wc -l)
if [ $TEST_COUNT -gt 0 ]; then
    print_status 0 "Found $TEST_COUNT test files"
else
    print_warning "No test files found"
fi

# Check for test categories
if [ -f "src/test/java/com/ksa/financing/service/template/unit/ExampleAggregateTest.java" ]; then
    print_status 0 "Unit tests present"
else
    print_warning "No unit tests found"
fi

if [ -f "src/test/java/com/ksa/financing/service/template/integration/ExampleIntegrationTest.java" ]; then
    print_status 0 "Integration tests present"
else
    print_warning "No integration tests found"
fi

echo ""
echo "========================================="
echo "Validation Results"
echo "========================================="

if [ $ERRORS -eq 0 ]; then
    if [ $WARNINGS -eq 0 ]; then
        echo -e "${GREEN}✓ All checks passed!${NC}"
        echo "The service template is properly configured."
    else
        echo -e "${GREEN}✓ Basic structure is valid${NC}"
        echo -e "${YELLOW}⚠ $WARNINGS warnings found${NC}"
        echo "The template is usable but could be improved."
    fi
    exit 0
else
    echo -e "${RED}✗ $ERRORS errors found${NC}"
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}⚠ $WARNINGS warnings found${NC}"
    fi
    echo "Please fix the errors before using the template."
    exit 1
fi