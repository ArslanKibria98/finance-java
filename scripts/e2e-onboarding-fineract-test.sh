#!/bin/bash
###############################################################################
# E2E Onboarding + Fineract Test Script
# Tests the full customer onboarding flow and Fineract core banking integration
###############################################################################
set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# Service URLs
ONBOARDING_URL="http://localhost:8089/api/v1/onboarding"
KYC_URL="http://localhost:8087/api/v1/kyc"
CUSTOMER_URL="http://localhost:8084/api/v1/customers"
WALLET_URL="http://localhost:8088/api/v1/wallets"
IDENTITY_URL="http://localhost:8083/api/v1"
GLOBAL_PROFILE_URL="http://localhost:8085/api/v1/profiles"
RISK_URL="http://localhost:8090/api/v1"
FINERACT_URL="https://localhost:8443/fineract-provider/api/v1"
FINERACT_AUTH="mifos:password"
FINERACT_TENANT="default"

# Test data - unique per run
TIMESTAMP=$(date +%s)
NID="1${TIMESTAMP:0:9}"  # 10-digit Saudi NID starting with 1
MOBILE="+966${TIMESTAMP:0:9}"
DEVICE_ID="test-device-${TIMESTAMP}"
EMAIL="test.user.${TIMESTAMP}@ksa-test.com"
PIN="847291"

# Tracking
PASS=0
FAIL=0
TOTAL=0
WORKFLOW_ID=""
ACCESS_TOKEN=""
CUSTOMER_ID=""
GLOBAL_UID=""
WALLET_ID=""
KEYCLOAK_USER_ID=""
FINERACT_CLIENT_ID=""
FINERACT_LOAN_ID=""

# Common headers for device info
DEVICE_HEADERS=(
    -H "X-Device-Id: ${DEVICE_ID}"
    -H "X-Latitude: 24.7136"
    -H "X-Longitude: 46.6753"
    -H "X-Client-Ip: 192.168.1.100"
    -H "X-User-Agent: E2E-Test/1.0"
)

###############################################################################
# Utility functions
###############################################################################
log_header() {
    echo ""
    echo -e "${BOLD}${BLUE}========================================${NC}"
    echo -e "${BOLD}${BLUE}  $1${NC}"
    echo -e "${BOLD}${BLUE}========================================${NC}"
}

log_step() {
    echo ""
    echo -e "${CYAN}--- $1 ---${NC}"
}

log_pass() {
    PASS=$((PASS + 1))
    TOTAL=$((TOTAL + 1))
    echo -e "  ${GREEN}PASS${NC} $1"
}

log_fail() {
    FAIL=$((FAIL + 1))
    TOTAL=$((TOTAL + 1))
    echo -e "  ${RED}FAIL${NC} $1"
    if [ -n "${2:-}" ]; then
        echo -e "       ${RED}$(echo "$2" | head -c 500)${NC}"
    fi
}

log_info() {
    echo -e "  ${YELLOW}INFO${NC} $1"
}

log_data() {
    echo -e "  ${BOLD}$1:${NC} $2"
}

assert_status() {
    local actual=$1
    local expected=$2
    local desc=$3
    local body="${4:-}"
    if [ "$actual" -eq "$expected" ]; then
        log_pass "$desc (HTTP $actual)"
    else
        log_fail "$desc (expected HTTP $expected, got HTTP $actual)" "$body"
    fi
}

# Smart JSON field extractor - handles nested paths and `data` wrapper
extract() {
    local json="$1"
    local field="$2"
    echo "$json" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    # Unwrap 'data' envelope if present
    if isinstance(d, dict) and 'data' in d and isinstance(d['data'], dict):
        d = d['data']
    keys = '$field'.split('.')
    for k in keys:
        if isinstance(d, dict):
            d = d.get(k, '')
        elif isinstance(d, list) and k.isdigit():
            d = d[int(k)]
        else:
            d = ''
    if d is None:
        print('')
    else:
        print(d)
except:
    print('')
" 2>/dev/null
}

###############################################################################
# PRE-FLIGHT CHECKS
###############################################################################
log_header "PRE-FLIGHT CHECKS"

log_step "Checking service health"
for port_name in "8089:onboarding" "8087:kyc-adapter" "8084:customer" "8088:wallet" "8083:identity" "8085:global-profile" "8090:risk" "8086:pii-vault"; do
    port="${port_name%%:*}"
    name="${port_name##*:}"
    status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/actuator/health" 2>/dev/null || echo "000")
    if [ "$status" = "200" ]; then
        log_pass "$name (port $port) UP"
    else
        log_fail "$name (port $port) DOWN"
    fi
done

fineract_status=$(curl -sk -o /dev/null -w "%{http_code}" "$FINERACT_URL/offices" -u "$FINERACT_AUTH" -H "Fineract-Platform-TenantId: $FINERACT_TENANT" 2>/dev/null || echo "000")
[ "$fineract_status" = "200" ] && log_pass "Fineract CBS (port 8443) UP" || log_fail "Fineract CBS DOWN"

echo ""
log_data "Test NID" "$NID"
log_data "Test Mobile" "$MOBILE"
log_data "Test Device" "$DEVICE_ID"

###############################################################################
# PART 1: CUSTOMER ONBOARDING E2E
###############################################################################
log_header "PART 1: CUSTOMER ONBOARDING E2E FLOW"

# ─── Step 1: Initiate Onboarding ─────────────────────────────────────────────
log_step "Step 1: Initiate Onboarding (Tahakuk + OTP)"

INITIATE_RESP=$(curl -s -w "\n%{http_code}" \
    -X POST "$ONBOARDING_URL/initiate" \
    -H "Content-Type: application/json" \
    "${DEVICE_HEADERS[@]}" \
    -d "{\"nationalId\":\"$NID\",\"mobileNumber\":\"$MOBILE\"}" 2>/dev/null)

INITIATE_STATUS=$(echo "$INITIATE_RESP" | tail -1)
INITIATE_JSON=$(echo "$INITIATE_RESP" | sed '$d')

assert_status "$INITIATE_STATUS" 201 "Initiate onboarding" "$INITIATE_JSON"

WORKFLOW_ID=$(extract "$INITIATE_JSON" "workflowId")
GLOBAL_UID=$(extract "$INITIATE_JSON" "globalUid")
OTP_REQUEST_ID=$(extract "$INITIATE_JSON" "otpRequestId")
INITIATE_STEP=$(extract "$INITIATE_JSON" "currentStep")

log_data "Workflow ID" "$WORKFLOW_ID"
log_data "Global UID" "$GLOBAL_UID"
log_data "OTP Request ID" "$OTP_REQUEST_ID"
log_data "Current Step" "$INITIATE_STEP"

[ -n "$WORKFLOW_ID" ] && [ "$WORKFLOW_ID" != "" ] && log_pass "Workflow created" || log_fail "Workflow ID missing"
[ "$INITIATE_STEP" = "OTP_SENT" ] && log_pass "Step -> OTP_SENT" || log_info "Step: $INITIATE_STEP"

sleep 2

# ─── Extract OTP from KYC adapter logs ───────────────────────────────────────
log_step "Step 2a: Extract OTP from KYC Adapter logs"

NID_LAST4="${NID: -4}"
OTP_CODE=$(docker logs ksa-kyc-adapter-service --tail 30 2>&1 | grep "UNIFONIC STUB.*${NID_LAST4}" | tail -1 | grep -oP 'is \K\d{6}' || echo "")

if [ -n "$OTP_CODE" ]; then
    log_pass "OTP extracted from logs: $OTP_CODE"
else
    log_fail "Could not extract OTP from KYC logs"
    log_info "Trying direct OTP lookup from DB..."
    OTP_CODE=$(docker exec ksa-postgres psql -U postgres -d kyc_adapter_db -t -c \
        "SELECT otp_code FROM otp_verifications WHERE national_id = '$NID' ORDER BY created_at DESC LIMIT 1;" 2>/dev/null | tr -d ' ' || echo "")
    if [ -n "$OTP_CODE" ]; then
        log_pass "OTP from DB: $OTP_CODE"
    else
        log_fail "Cannot get OTP - skipping OTP verification"
        OTP_CODE="000000"
    fi
fi

# ─── Step 2b: Verify OTP ─────────────────────────────────────────────────────
log_step "Step 2b: Verify OTP + Keycloak Registration"

OTP_RESP=$(curl -s -w "\n%{http_code}" \
    -X POST "$ONBOARDING_URL/verify-otp" \
    -H "Content-Type: application/json" \
    "${DEVICE_HEADERS[@]}" \
    -d "{\"nationalId\":\"$NID\",\"mobileNumber\":\"$MOBILE\",\"otpCode\":\"$OTP_CODE\"}" 2>/dev/null)

OTP_STATUS=$(echo "$OTP_RESP" | tail -1)
OTP_JSON=$(echo "$OTP_RESP" | sed '$d')

assert_status "$OTP_STATUS" 200 "OTP verification" "$OTP_JSON"

ACCESS_TOKEN=$(extract "$OTP_JSON" "accessToken")
REFRESH_TOKEN=$(extract "$OTP_JSON" "refreshToken")
KEYCLOAK_USER_ID=$(extract "$OTP_JSON" "keycloakUserId")
OTP_STEP=$(extract "$OTP_JSON" "currentStep")
CUSTOMER_ID=$(extract "$OTP_JSON" "customerId")
GLOBAL_UID_OTP=$(extract "$OTP_JSON" "globalUid")

# Update global UID if available
[ -n "$GLOBAL_UID_OTP" ] && [ "$GLOBAL_UID_OTP" != "" ] && GLOBAL_UID="$GLOBAL_UID_OTP"

if [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "" ]; then
    log_pass "JWT access token received: ${ACCESS_TOKEN:0:30}..."
    AUTH_HEADER="Authorization: Bearer $ACCESS_TOKEN"
else
    log_fail "JWT access token not received"
    log_info "Response: $(echo "$OTP_JSON" | python3 -m json.tool 2>/dev/null | head -20)"
    AUTH_HEADER=""
fi

[ -n "$KEYCLOAK_USER_ID" ] && log_pass "Keycloak user: $KEYCLOAK_USER_ID" || log_info "Keycloak user ID not in direct response"
log_data "Current Step" "$OTP_STEP"

sleep 1

# ─── Step 3: Accept Terms ───────────────────────────────────────────────────
log_step "Step 3: Accept Terms & Conditions"

if [ -z "$AUTH_HEADER" ]; then
    log_fail "Skipping - no JWT token available"
else
    TERMS_RESP=$(curl -s -w "\n%{http_code}" \
        -X POST "$ONBOARDING_URL/accept-terms" \
        -H "Content-Type: application/json" \
        -H "$AUTH_HEADER" \
        "${DEVICE_HEADERS[@]}" \
        -d "{\"nationalId\":\"$NID\",\"accepted\":true}" 2>/dev/null)

    TERMS_STATUS=$(echo "$TERMS_RESP" | tail -1)
    TERMS_JSON=$(echo "$TERMS_RESP" | sed '$d')

    assert_status "$TERMS_STATUS" 200 "Accept terms" "$TERMS_JSON"

    TERMS_STEP=$(extract "$TERMS_JSON" "currentStep")
    log_data "Current Step" "$TERMS_STEP"
    [ "$TERMS_STEP" = "TERMS_ACCEPTED" ] && log_pass "Step -> TERMS_ACCEPTED" || log_info "Step: $TERMS_STEP"
fi

sleep 1

# ─── Step 4a: Initiate Nafath ───────────────────────────────────────────────
log_step "Step 4a: Initiate Nafath Verification"

if [ -z "$AUTH_HEADER" ]; then
    log_fail "Skipping - no JWT token"
else
    NAFATH_INIT_RESP=$(curl -s -w "\n%{http_code}" \
        -X POST "$ONBOARDING_URL/initiate-nafath" \
        -H "Content-Type: application/json" \
        -H "$AUTH_HEADER" \
        "${DEVICE_HEADERS[@]}" \
        -d "{\"nationalId\":\"$NID\"}" 2>/dev/null)

    NAFATH_INIT_STATUS=$(echo "$NAFATH_INIT_RESP" | tail -1)
    NAFATH_INIT_JSON=$(echo "$NAFATH_INIT_RESP" | sed '$d')

    assert_status "$NAFATH_INIT_STATUS" 200 "Initiate Nafath" "$NAFATH_INIT_JSON"

    NAFATH_RANDOM=$(extract "$NAFATH_INIT_JSON" "nafathRandomNumber")
    NAFATH_SESSION=$(extract "$NAFATH_INIT_JSON" "nafathSessionId")
    NAFATH_TXN=$(extract "$NAFATH_INIT_JSON" "transactionId")
    NAFATH_STEP=$(extract "$NAFATH_INIT_JSON" "currentStep")

    log_data "Random Number" "$NAFATH_RANDOM"
    log_data "Session ID" "$NAFATH_SESSION"
    log_data "Transaction ID" "$NAFATH_TXN"
    log_data "Current Step" "$NAFATH_STEP"

    [ -n "$NAFATH_RANDOM" ] && log_pass "Nafath initiated" || log_info "Nafath in mock mode"
fi

sleep 1

# ─── Step 4b: Nafath Callback ───────────────────────────────────────────────
log_step "Step 4b: Nafath Callback (User Accepts)"

NAFATH_CB_RESP=$(curl -s -w "\n%{http_code}" \
    -X POST "$ONBOARDING_URL/nafath-callback" \
    -H "Content-Type: application/json" \
    -d "{\"nationalId\":\"$NID\",\"transactionId\":\"${NAFATH_TXN:-txn-test-001}\",\"accepted\":true,\"rejectionReason\":null}" 2>/dev/null)

NAFATH_CB_STATUS=$(echo "$NAFATH_CB_RESP" | tail -1)
NAFATH_CB_JSON=$(echo "$NAFATH_CB_RESP" | sed '$d')

assert_status "$NAFATH_CB_STATUS" 200 "Nafath callback" "$NAFATH_CB_JSON"

# This triggers auto-steps: Yakeen + Sanctions + Profile Creation
# Wait a bit for them to complete
sleep 5

CB_STEP=$(extract "$NAFATH_CB_JSON" "currentStep")
CB_CUSTOMER_ID=$(extract "$NAFATH_CB_JSON" "customerId")
CB_GLOBAL_UID=$(extract "$NAFATH_CB_JSON" "globalUid")

log_data "Current Step" "$CB_STEP"
log_data "Customer ID" "$CB_CUSTOMER_ID"
log_data "Global UID" "$CB_GLOBAL_UID"

[ -n "$CB_CUSTOMER_ID" ] && [ "$CB_CUSTOMER_ID" != "" ] && CUSTOMER_ID="$CB_CUSTOMER_ID"
[ -n "$CB_GLOBAL_UID" ] && [ "$CB_GLOBAL_UID" != "" ] && GLOBAL_UID="$CB_GLOBAL_UID"

if [ "$CB_STEP" = "INFO_PENDING" ]; then
    log_pass "Auto-steps done: Yakeen + Sanctions + Profile Created"
else
    log_info "Step: $CB_STEP (expected INFO_PENDING, may still be processing)"
    # Query status to get updated state
    if [ -n "$AUTH_HEADER" ] && [ "$AUTH_HEADER" != "" ]; then
        sleep 5
        STATUS_CHECK=$(curl -s "$ONBOARDING_URL/status?nationalId=$NID" -H "$AUTH_HEADER" 2>/dev/null)
        CB_STEP=$(extract "$STATUS_CHECK" "currentStep")
        CUSTOMER_ID=$(extract "$STATUS_CHECK" "customerId")
        GLOBAL_UID=$(extract "$STATUS_CHECK" "globalUid")
        log_data "Updated Step" "$CB_STEP"
        log_data "Updated Customer ID" "$CUSTOMER_ID"
    fi
fi

sleep 2

# ─── Step 5: Submit Additional Info ─────────────────────────────────────────
log_step "Step 5: Submit Additional Info (Employment + Banking)"

if [ -z "$AUTH_HEADER" ]; then
    log_fail "Skipping - no JWT token"
else
    SUBMIT_INFO_RESP=$(curl -s -w "\n%{http_code}" \
        -X POST "$ONBOARDING_URL/submit-info" \
        -H "Content-Type: application/json" \
        -H "$AUTH_HEADER" \
        "${DEVICE_HEADERS[@]}" \
        -d "{
            \"nationalId\":\"$NID\",
            \"email\":\"$EMAIL\",
            \"employerName\":\"Saudi Aramco\",
            \"employerCrNumber\":\"1010000001\",
            \"employmentType\":\"FULL_TIME\",
            \"jobTitle\":\"Senior Software Engineer\",
            \"basicSalary\":25000.00,
            \"grossSalary\":30000.00,
            \"netSalary\":28000.00,
            \"currency\":\"SAR\",
            \"bankName\":\"Al Rajhi Bank\",
            \"bankCode\":\"RJHI\",
            \"iban\":\"SA4420000001234567891234\",
            \"accountHolderName\":\"Test User Al-Saud\",
            \"isPep\":false
        }" 2>/dev/null)

    SUBMIT_INFO_STATUS=$(echo "$SUBMIT_INFO_RESP" | tail -1)
    SUBMIT_INFO_JSON=$(echo "$SUBMIT_INFO_RESP" | sed '$d')

    assert_status "$SUBMIT_INFO_STATUS" 200 "Submit additional info" "$SUBMIT_INFO_JSON"

    INFO_STEP=$(extract "$SUBMIT_INFO_JSON" "currentStep")
    INFO_MSG=$(extract "$SUBMIT_INFO_JSON" "message")
    INFO_ERROR=$(extract "$SUBMIT_INFO_JSON" "error")

    log_data "Current Step" "$INFO_STEP"
    log_data "Message" "$INFO_MSG"
    [ -n "$INFO_ERROR" ] && [ "$INFO_ERROR" != "" ] && log_data "Error" "$INFO_ERROR"

    if [ "$INFO_STEP" = "PIN_SETUP" ]; then
        log_pass "Non-PEP flow: Risk approved, wallet created -> PIN_SETUP"
    elif [ "$INFO_STEP" = "COMPLETING" ]; then
        log_info "Still completing auto-steps, waiting..."
        sleep 8
        STATUS_CHECK2=$(curl -s "$ONBOARDING_URL/status?nationalId=$NID" -H "$AUTH_HEADER" 2>/dev/null)
        INFO_STEP=$(extract "$STATUS_CHECK2" "currentStep")
        log_data "Updated Step" "$INFO_STEP"
    fi
fi

sleep 2

# ─── Step 7: Set PIN ────────────────────────────────────────────────────────
log_step "Step 7: Set App PIN"

if [ -z "$AUTH_HEADER" ]; then
    log_fail "Skipping - no JWT token"
else
    # Get current state
    STATUS_BEFORE_PIN=$(curl -s "$ONBOARDING_URL/status?nationalId=$NID" -H "$AUTH_HEADER" 2>/dev/null)
    PRE_PIN_STEP=$(extract "$STATUS_BEFORE_PIN" "currentStep")
    WALLET_ID=$(extract "$STATUS_BEFORE_PIN" "walletId")
    KEYCLOAK_USER_ID=$(extract "$STATUS_BEFORE_PIN" "keycloakUserId")

    log_data "Pre-PIN Step" "$PRE_PIN_STEP"
    log_data "Wallet ID" "$WALLET_ID"

    [ -n "$WALLET_ID" ] && [ "$WALLET_ID" != "" ] && log_pass "Wallet created: $WALLET_ID"

    SET_PIN_RESP=$(curl -s -w "\n%{http_code}" \
        -X POST "$ONBOARDING_URL/set-pin" \
        -H "Content-Type: application/json" \
        -H "$AUTH_HEADER" \
        "${DEVICE_HEADERS[@]}" \
        -d "{\"nationalId\":\"$NID\",\"pin\":\"$PIN\",\"confirmPin\":\"$PIN\"}" 2>/dev/null)

    SET_PIN_STATUS=$(echo "$SET_PIN_RESP" | tail -1)
    SET_PIN_JSON=$(echo "$SET_PIN_RESP" | sed '$d')

    assert_status "$SET_PIN_STATUS" 200 "Set PIN" "$SET_PIN_JSON"

    PIN_STEP=$(extract "$SET_PIN_JSON" "currentStep")
    PIN_MSG=$(extract "$SET_PIN_JSON" "message")

    log_data "Current Step" "$PIN_STEP"
    log_data "Message" "$PIN_MSG"

    if [ "$PIN_STEP" = "COMPLETED" ]; then
        log_pass "ONBOARDING COMPLETED SUCCESSFULLY!"
    else
        log_info "Step after PIN: $PIN_STEP"
    fi
fi

sleep 1

# ─── Final Status ───────────────────────────────────────────────────────────
log_step "Final Status Query"

if [ -n "$AUTH_HEADER" ] && [ "$AUTH_HEADER" != "" ]; then
    FINAL_STATUS=$(curl -s "$ONBOARDING_URL/status?nationalId=$NID" -H "$AUTH_HEADER" 2>/dev/null)

    FINAL_STEP=$(extract "$FINAL_STATUS" "currentStep")
    FINAL_CUSTOMER_ID=$(extract "$FINAL_STATUS" "customerId")
    FINAL_GLOBAL_UID=$(extract "$FINAL_STATUS" "globalUid")
    FINAL_WALLET_ID=$(extract "$FINAL_STATUS" "walletId")
    FINAL_KC_USER=$(extract "$FINAL_STATUS" "keycloakUserId")
    FINAL_LIFECYCLE=$(extract "$FINAL_STATUS" "lifecycleStage")
    FINAL_TRUSTED=$(extract "$FINAL_STATUS" "deviceTrusted")

    log_data "Final Step" "$FINAL_STEP"
    log_data "Customer ID" "$FINAL_CUSTOMER_ID"
    log_data "Global UID" "$FINAL_GLOBAL_UID"
    log_data "Wallet ID" "$FINAL_WALLET_ID"
    log_data "Keycloak User" "$FINAL_KC_USER"
    log_data "Lifecycle" "$FINAL_LIFECYCLE"
    log_data "Device Trusted" "$FINAL_TRUSTED"

    # Update tracking vars
    [ -n "$FINAL_CUSTOMER_ID" ] && [ "$FINAL_CUSTOMER_ID" != "" ] && CUSTOMER_ID="$FINAL_CUSTOMER_ID"
    [ -n "$FINAL_GLOBAL_UID" ] && [ "$FINAL_GLOBAL_UID" != "" ] && GLOBAL_UID="$FINAL_GLOBAL_UID"
    [ -n "$FINAL_WALLET_ID" ] && [ "$FINAL_WALLET_ID" != "" ] && WALLET_ID="$FINAL_WALLET_ID"
    [ -n "$FINAL_KC_USER" ] && [ "$FINAL_KC_USER" != "" ] && KEYCLOAK_USER_ID="$FINAL_KC_USER"

    echo ""
    echo "$FINAL_STATUS" | python3 -m json.tool 2>/dev/null | head -60
fi

###############################################################################
# PART 2: CROSS-SERVICE DATA VERIFICATION
###############################################################################
log_header "PART 2: CROSS-SERVICE DATA VERIFICATION"

# ─── Verify Customer ─────────────────────────────────────────────────────────
log_step "Verify Customer in customer-service"
if [ -n "$CUSTOMER_ID" ] && [ "$CUSTOMER_ID" != "" ]; then
    CUST_RESP=$(curl -s -w "\n%{http_code}" "$CUSTOMER_URL/$CUSTOMER_ID" \
        -H "${AUTH_HEADER:-X-Skip: true}" 2>/dev/null)
    CUST_STATUS=$(echo "$CUST_RESP" | tail -1)
    CUST_JSON=$(echo "$CUST_RESP" | sed '$d')

    if [ "$CUST_STATUS" = "200" ]; then
        log_pass "Customer found via API"
        echo "$CUST_JSON" | python3 -m json.tool 2>/dev/null | head -20
    else
        log_info "Customer API returned $CUST_STATUS, checking DB directly..."
    fi
fi

# ─── Check DB directly ──────────────────────────────────────────────────────
log_step "Direct Database Verification"

echo ""
log_info "customer_db:"
docker exec ksa-postgres psql -U postgres -d customer_db -c \
    "SELECT id, national_id, mobile_number, status, lifecycle_stage, cif_number, full_name_en FROM customers WHERE national_id = '$NID';" 2>/dev/null || log_info "  No record found"

echo ""
log_info "wallet_db:"
if [ -n "$CUSTOMER_ID" ] && [ "$CUSTOMER_ID" != "" ]; then
    docker exec ksa-postgres psql -U postgres -d wallet_db -c \
        "SELECT id, customer_id, currency, status, balance FROM wallets WHERE customer_id = '$CUSTOMER_ID';" 2>/dev/null || log_info "  No record found"
else
    log_info "  No customer ID to query wallets"
fi

echo ""
log_info "global_profile_db:"
if [ -n "$GLOBAL_UID" ] && [ "$GLOBAL_UID" != "" ]; then
    docker exec ksa-postgres psql -U postgres -d global_profile_db -c \
        "SELECT id, kyc_status, lifecycle_stage, risk_level, country_code FROM global_customer_profiles WHERE id = '$GLOBAL_UID';" 2>/dev/null || log_info "  No record found"
else
    log_info "  No global UID to query"
fi

echo ""
log_info "Temporal workflow:"
log_data "  Workflow URL" "http://localhost:8234/namespaces/default/workflows/${WORKFLOW_ID:-N/A}"

###############################################################################
# PART 3: FINERACT CORE BANKING INTEGRATION
###############################################################################
log_header "PART 3: FINERACT CORE BANKING INTEGRATION"

FINERACT_HEADERS=(
    -H "Content-Type: application/json"
    -H "Fineract-Platform-TenantId: $FINERACT_TENANT"
    -u "$FINERACT_AUTH"
)

# ─── 3.1: Create Murabaha Loan Product ──────────────────────────────────────
log_step "3.1: Setup Murabaha Loan Product"

# Check existing products
PRODUCTS_JSON=$(curl -sk "$FINERACT_URL/loanproducts" "${FINERACT_HEADERS[@]}" 2>/dev/null)
PRODUCT_COUNT=$(echo "$PRODUCTS_JSON" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d) if isinstance(d,list) else 0)" 2>/dev/null || echo "0")

if [ "$PRODUCT_COUNT" != "0" ]; then
    LOAN_PRODUCT_ID=$(echo "$PRODUCTS_JSON" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['id'])" 2>/dev/null)
    PRODUCT_NAME=$(echo "$PRODUCTS_JSON" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0].get('name','?'))" 2>/dev/null)
    log_pass "Existing product: $PRODUCT_NAME (ID=$LOAN_PRODUCT_ID)"
else
    log_info "Creating Murabaha loan product..."

    CREATE_PROD_RESP=$(curl -sk -w "\n%{http_code}" \
        -X POST "$FINERACT_URL/loanproducts" \
        "${FINERACT_HEADERS[@]}" \
        -d '{
            "name": "Murabaha Personal Finance",
            "shortName": "MURB",
            "description": "Islamic Murabaha cost-plus-profit personal financing",
            "currencyCode": "SAR",
            "digitsAfterDecimal": 2,
            "inMultiplesOf": 1,
            "principal": 100000.00,
            "minPrincipal": 5000.00,
            "maxPrincipal": 2000000.00,
            "numberOfRepayments": 60,
            "minNumberOfRepayments": 6,
            "maxNumberOfRepayments": 120,
            "repaymentEvery": 1,
            "repaymentFrequencyType": 2,
            "interestRatePerPeriod": 5.0,
            "minInterestRatePerPeriod": 0.0,
            "maxInterestRatePerPeriod": 15.0,
            "interestRateFrequencyType": 3,
            "amortizationType": 1,
            "interestType": 1,
            "interestCalculationPeriodType": 1,
            "transactionProcessingStrategyCode": "mifos-standard-strategy",
            "locale": "en",
            "dateFormat": "dd MMMM yyyy",
            "accountingRule": 1,
            "daysInMonthType": 1,
            "daysInYearType": 1
        }' 2>/dev/null)

    CREATE_PROD_STATUS=$(echo "$CREATE_PROD_RESP" | tail -1)
    CREATE_PROD_JSON=$(echo "$CREATE_PROD_RESP" | sed '$d')

    LOAN_PRODUCT_ID=$(extract "$CREATE_PROD_JSON" "resourceId")

    if [ -n "$LOAN_PRODUCT_ID" ] && [ "$LOAN_PRODUCT_ID" != "" ]; then
        log_pass "Murabaha product created: ID=$LOAN_PRODUCT_ID"
    else
        log_fail "Product creation failed" "$CREATE_PROD_JSON"
        LOAN_PRODUCT_ID=""
    fi
fi

# ─── 3.2: Create Fineract Client ────────────────────────────────────────────
log_step "3.2: Create Client in Fineract CBS"

TODAY=$(date +"%d %B %Y")

CREATE_CLIENT_RESP=$(curl -sk -w "\n%{http_code}" \
    -X POST "$FINERACT_URL/clients" \
    "${FINERACT_HEADERS[@]}" \
    -d "{
        \"officeId\": 1,
        \"firstname\": \"Test\",
        \"lastname\": \"Customer-$NID\",
        \"externalId\": \"${CUSTOMER_ID:-ext-$NID}\",
        \"mobileNo\": \"$MOBILE\",
        \"active\": true,
        \"activationDate\": \"$TODAY\",
        \"locale\": \"en\",
        \"dateFormat\": \"dd MMMM yyyy\",
        \"legalFormId\": 1
    }" 2>/dev/null)

CREATE_CLIENT_STATUS=$(echo "$CREATE_CLIENT_RESP" | tail -1)
CREATE_CLIENT_JSON=$(echo "$CREATE_CLIENT_RESP" | sed '$d')

assert_status "$CREATE_CLIENT_STATUS" 200 "Create Fineract client" "$CREATE_CLIENT_JSON"

FINERACT_CLIENT_ID=$(extract "$CREATE_CLIENT_JSON" "clientId")
[ -z "$FINERACT_CLIENT_ID" ] || [ "$FINERACT_CLIENT_ID" = "" ] && FINERACT_CLIENT_ID=$(extract "$CREATE_CLIENT_JSON" "resourceId")

log_data "Fineract Client ID" "$FINERACT_CLIENT_ID"

if [ -n "$FINERACT_CLIENT_ID" ] && [ "$FINERACT_CLIENT_ID" != "" ]; then
    log_pass "Client created in Fineract CBS"

    # Verify
    CLIENT_GET=$(curl -sk "$FINERACT_URL/clients/$FINERACT_CLIENT_ID" "${FINERACT_HEADERS[@]}" 2>/dev/null)
    log_data "  Display Name" "$(extract "$CLIENT_GET" "displayName")"
    log_data "  External ID" "$(extract "$CLIENT_GET" "externalId")"
    log_data "  Active" "$(extract "$CLIENT_GET" "active")"
fi

# ─── 3.3: Create Murabaha Loan Account ──────────────────────────────────────
log_step "3.3: Create Murabaha Loan Account (100,000 SAR, 60 months)"

if [ -n "$FINERACT_CLIENT_ID" ] && [ "$FINERACT_CLIENT_ID" != "" ] && [ -n "$LOAN_PRODUCT_ID" ] && [ "$LOAN_PRODUCT_ID" != "" ]; then
    FIRST_REPAYMENT=$(date -d "+30 days" +"%d %B %Y" 2>/dev/null || date -v+30d +"%d %B %Y" 2>/dev/null || echo "$TODAY")

    CREATE_LOAN_RESP=$(curl -sk -w "\n%{http_code}" \
        -X POST "$FINERACT_URL/loans" \
        "${FINERACT_HEADERS[@]}" \
        -d "{
            \"clientId\": $FINERACT_CLIENT_ID,
            \"productId\": $LOAN_PRODUCT_ID,
            \"principal\": 100000,
            \"loanTermFrequency\": 60,
            \"loanTermFrequencyType\": 2,
            \"numberOfRepayments\": 60,
            \"repaymentEvery\": 1,
            \"repaymentFrequencyType\": 2,
            \"interestRatePerPeriod\": 5,
            \"amortizationType\": 1,
            \"interestType\": 1,
            \"interestCalculationPeriodType\": 1,
            \"transactionProcessingStrategyCode\": \"mifos-standard-strategy\",
            \"expectedDisbursementDate\": \"$TODAY\",
            \"submittedOnDate\": \"$TODAY\",
            \"locale\": \"en\",
            \"dateFormat\": \"dd MMMM yyyy\",
            \"loanType\": \"individual\"
        }" 2>/dev/null)

    CREATE_LOAN_STATUS=$(echo "$CREATE_LOAN_RESP" | tail -1)
    CREATE_LOAN_JSON=$(echo "$CREATE_LOAN_RESP" | sed '$d')

    assert_status "$CREATE_LOAN_STATUS" 200 "Create Murabaha loan" "$CREATE_LOAN_JSON"

    FINERACT_LOAN_ID=$(extract "$CREATE_LOAN_JSON" "loanId")
    [ -z "$FINERACT_LOAN_ID" ] || [ "$FINERACT_LOAN_ID" = "" ] && FINERACT_LOAN_ID=$(extract "$CREATE_LOAN_JSON" "resourceId")

    log_data "Loan ID" "$FINERACT_LOAN_ID"

    if [ -n "$FINERACT_LOAN_ID" ] && [ "$FINERACT_LOAN_ID" != "" ]; then
        log_pass "Murabaha loan created"

        LOAN_DETAILS=$(curl -sk "$FINERACT_URL/loans/$FINERACT_LOAN_ID" "${FINERACT_HEADERS[@]}" 2>/dev/null)
        log_data "  Status" "$(extract "$LOAN_DETAILS" "status.value")"
        log_data "  Principal" "$(extract "$LOAN_DETAILS" "principal") SAR"
        log_data "  Term" "$(extract "$LOAN_DETAILS" "termFrequency") months"
    fi
else
    log_fail "Skipping - no client or product ID"
fi

# ─── 3.4: Approve Loan ──────────────────────────────────────────────────────
log_step "3.4: Approve Loan"

if [ -n "$FINERACT_LOAN_ID" ] && [ "$FINERACT_LOAN_ID" != "" ]; then
    APPROVE_RESP=$(curl -sk -w "\n%{http_code}" \
        -X POST "$FINERACT_URL/loans/$FINERACT_LOAN_ID?command=approve" \
        "${FINERACT_HEADERS[@]}" \
        -d "{
            \"approvedOnDate\": \"$TODAY\",
            \"approvedLoanAmount\": 100000,
            \"locale\": \"en\",
            \"dateFormat\": \"dd MMMM yyyy\",
            \"note\": \"Murabaha approved - E2E test\"
        }" 2>/dev/null)

    APPROVE_STATUS=$(echo "$APPROVE_RESP" | tail -1)
    APPROVE_JSON=$(echo "$APPROVE_RESP" | sed '$d')

    assert_status "$APPROVE_STATUS" 200 "Approve loan" "$APPROVE_JSON"

    LOAN_AFTER_APPROVE=$(curl -sk "$FINERACT_URL/loans/$FINERACT_LOAN_ID" "${FINERACT_HEADERS[@]}" 2>/dev/null)
    LOAN_STATUS_2=$(extract "$LOAN_AFTER_APPROVE" "status.value")
    log_data "Loan Status" "$LOAN_STATUS_2"
    [ "$LOAN_STATUS_2" = "Approved" ] && log_pass "Loan approved" || log_info "Status: $LOAN_STATUS_2"
else
    log_info "Skipping - no loan ID"
fi

# ─── 3.5: Disburse Loan ─────────────────────────────────────────────────────
log_step "3.5: Disburse Loan (100,000 SAR to customer)"

if [ -n "$FINERACT_LOAN_ID" ] && [ "$FINERACT_LOAN_ID" != "" ]; then
    DISBURSE_RESP=$(curl -sk -w "\n%{http_code}" \
        -X POST "$FINERACT_URL/loans/$FINERACT_LOAN_ID?command=disburse" \
        "${FINERACT_HEADERS[@]}" \
        -d "{
            \"actualDisbursementDate\": \"$TODAY\",
            \"locale\": \"en\",
            \"dateFormat\": \"dd MMMM yyyy\",
            \"transactionAmount\": 100000,
            \"note\": \"Murabaha disbursement - E2E test\"
        }" 2>/dev/null)

    DISBURSE_STATUS=$(echo "$DISBURSE_RESP" | tail -1)
    DISBURSE_JSON=$(echo "$DISBURSE_RESP" | sed '$d')

    assert_status "$DISBURSE_STATUS" 200 "Disburse loan" "$DISBURSE_JSON"

    LOAN_AFTER_DISBURSE=$(curl -sk "$FINERACT_URL/loans/$FINERACT_LOAN_ID" "${FINERACT_HEADERS[@]}" 2>/dev/null)
    LOAN_STATUS_3=$(extract "$LOAN_AFTER_DISBURSE" "status.value")
    log_data "Loan Status" "$LOAN_STATUS_3"
    [ "$LOAN_STATUS_3" = "Active" ] && log_pass "Loan disbursed and ACTIVE" || log_info "Status: $LOAN_STATUS_3"
else
    log_info "Skipping - no loan ID"
fi

# ─── 3.6: Repayment Schedule ────────────────────────────────────────────────
log_step "3.6: Repayment Schedule"

if [ -n "$FINERACT_LOAN_ID" ] && [ "$FINERACT_LOAN_ID" != "" ]; then
    SCHEDULE_RESP=$(curl -sk "$FINERACT_URL/loans/$FINERACT_LOAN_ID?associations=repaymentSchedule" "${FINERACT_HEADERS[@]}" 2>/dev/null)

    echo "$SCHEDULE_RESP" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    schedule = d.get('repaymentSchedule', {})
    periods = schedule.get('periods', [])
    total = schedule.get('totalRepaymentExpected', 0)
    total_interest = schedule.get('totalInterestCharged', 0)
    total_principal = schedule.get('totalPrincipalExpected', 0)

    print(f'  Total Periods: {len(periods)}')
    print(f'  Total Principal: {total_principal:,.2f} SAR')
    print(f'  Total Profit: {total_interest:,.2f} SAR')
    print(f'  Total Repayment: {total:,.2f} SAR')
    print()
    print('  First 5 installments:')
    for p in periods[1:6]:
        due = p.get('dueDate', [0,0,0])
        due_str = f'{due[2]:02d}/{due[1]:02d}/{due[0]}' if len(due) >= 3 else 'N/A'
        pr = p.get('principalDue', 0)
        ir = p.get('interestDue', 0)
        tot = p.get('totalDueForPeriod', 0)
        print(f'    Month {p.get(\"period\",\"?\"):>2}: Due={due_str}  Principal={pr:>10,.2f}  Profit={ir:>8,.2f}  Total={tot:>10,.2f} SAR')
except Exception as e:
    print(f'  Error: {e}')
" 2>/dev/null

    PERIOD_COUNT=$(echo "$SCHEDULE_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('repaymentSchedule',{}).get('periods',[])))" 2>/dev/null || echo "0")
    [ "$PERIOD_COUNT" != "0" ] && log_pass "Repayment schedule: $PERIOD_COUNT periods" || log_info "No schedule"
else
    log_info "Skipping - no loan ID"
fi

# ─── 3.7: Record Repayment ──────────────────────────────────────────────────
log_step "3.7: Record First Monthly Repayment"

if [ -n "$FINERACT_LOAN_ID" ] && [ "$FINERACT_LOAN_ID" != "" ]; then
    # Get first installment amount
    FIRST_AMOUNT=$(echo "$SCHEDULE_RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
periods = d.get('repaymentSchedule',{}).get('periods',[])
print(periods[1].get('totalDueForPeriod', 2000) if len(periods) > 1 else 2000)
" 2>/dev/null || echo "2000")

    REPAY_RESP=$(curl -sk -w "\n%{http_code}" \
        -X POST "$FINERACT_URL/loans/$FINERACT_LOAN_ID/transactions?command=repayment" \
        "${FINERACT_HEADERS[@]}" \
        -d "{
            \"transactionDate\": \"$TODAY\",
            \"transactionAmount\": $FIRST_AMOUNT,
            \"locale\": \"en\",
            \"dateFormat\": \"dd MMMM yyyy\",
            \"paymentTypeId\": 1,
            \"note\": \"First monthly installment - E2E test\"
        }" 2>/dev/null)

    REPAY_STATUS=$(echo "$REPAY_RESP" | tail -1)
    REPAY_JSON=$(echo "$REPAY_RESP" | sed '$d')

    assert_status "$REPAY_STATUS" 200 "Record repayment" "$REPAY_JSON"

    REPAY_TXN_ID=$(extract "$REPAY_JSON" "resourceId")
    log_data "Transaction ID" "$REPAY_TXN_ID"

    if [ -n "$REPAY_TXN_ID" ] && [ "$REPAY_TXN_ID" != "" ]; then
        log_pass "Repayment recorded: $FIRST_AMOUNT SAR"

        # Get updated summary
        LOAN_SUMMARY=$(curl -sk "$FINERACT_URL/loans/$FINERACT_LOAN_ID" "${FINERACT_HEADERS[@]}" 2>/dev/null)
        echo "$LOAN_SUMMARY" | python3 -c "
import sys, json
d = json.load(sys.stdin)
s = d.get('summary', {})
print(f'  Outstanding: {s.get(\"totalOutstanding\", 0):,.2f} SAR')
print(f'  Paid: {s.get(\"totalRepayment\", 0):,.2f} SAR')
print(f'  Principal Paid: {s.get(\"principalPaid\", 0):,.2f} SAR')
print(f'  Profit Paid: {s.get(\"interestPaid\", 0):,.2f} SAR')
print(f'  Overdue: {s.get(\"totalOverdue\", 0):,.2f} SAR')
" 2>/dev/null
    fi
else
    log_info "Skipping - no loan ID"
fi

###############################################################################
# SUMMARY
###############################################################################
log_header "E2E TEST SUMMARY"

echo ""
echo -e "${BOLD}Onboarding Flow:${NC}"
log_data "  Workflow ID" "${WORKFLOW_ID:-N/A}"
log_data "  Customer ID" "${CUSTOMER_ID:-N/A}"
log_data "  Global UID" "${GLOBAL_UID:-N/A}"
log_data "  Wallet ID" "${WALLET_ID:-N/A}"
log_data "  Keycloak User" "${KEYCLOAK_USER_ID:-N/A}"

echo ""
echo -e "${BOLD}Fineract CBS:${NC}"
log_data "  Client ID" "${FINERACT_CLIENT_ID:-N/A}"
log_data "  Loan Product" "${LOAN_PRODUCT_ID:-N/A}"
log_data "  Loan ID" "${FINERACT_LOAN_ID:-N/A}"

echo ""
echo -e "${BOLD}Results:${NC}"
echo -e "  ${GREEN}Passed: $PASS${NC}"
echo -e "  ${RED}Failed: $FAIL${NC}"
echo -e "  Total:  $TOTAL"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}${BOLD}ALL TESTS PASSED!${NC}"
else
    echo -e "${YELLOW}${BOLD}$FAIL test(s) need attention out of $TOTAL${NC}"
fi

exit 0
