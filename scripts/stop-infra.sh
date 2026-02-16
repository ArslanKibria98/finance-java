#!/bin/bash

# Stop Infrastructure Services for KSA Islamic Financing Platform
# This script stops all running infrastructure services

set -e

echo "🛑 Stopping KSA Islamic Financing Platform Infrastructure..."
echo ""

# Navigate to the project root
cd "$(dirname "$0")/.."

# Stop all services
docker-compose down

echo ""
echo "✅ All infrastructure services stopped successfully!"
echo ""
echo "💡 To remove all data volumes, run: docker-compose down -v"
