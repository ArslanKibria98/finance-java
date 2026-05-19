#!/usr/bin/env bash
# Build all 12 active service JARs for QA.
# Usage:
#   ./scripts/qa/build-all.sh            # build everything (skip tests)
#   ./scripts/qa/build-all.sh --tests    # build with tests
set -euo pipefail

cd "$(dirname "$0")/../.."

SKIP_TESTS="-DskipTests"
if [ "${1:-}" = "--tests" ]; then
  SKIP_TESTS=""
fi

echo "==> Installing shared libraries..."
mvn -pl shared-libraries -am install -N -DskipTests
mvn -pl shared-libraries -am install -DskipTests

echo "==> Building all active services..."
mvn -pl \
  services/identity-service,\
services/customer-service,\
services/global-profile-service,\
services/kyc-adapter-service,\
services/wallet-service,\
services/risk-service,\
services/product-service,\
services/fraud-service,\
services/ledger-service,\
services/notification-service,\
services/lending-service,\
services/collections-service \
  -am clean package $SKIP_TESTS

echo "==> Build complete."
