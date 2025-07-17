#!/bin/bash

# Admin Workflow Test Script
# Tests admin login, product fetching, and other admin operations

set -e

# Configuration
BASE_URL="http://localhost:8080"
ADMIN_EMAIL="admin@example.com"
ADMIN_PASSWORD="admin123"
TEST_RESULTS_FILE="test_results_$(date +%Y%m%d_%H%M%S).log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${2:-$NC}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}" | tee -a "$TEST_RESULTS_FILE"
}

# Test function
test_endpoint() {
    local description="$1"
    local method="$2"
    local endpoint="$3"
    local headers="$4"
    local data="$5"
    local expected_status="$6"
    
    log "Testing: $description" "$YELLOW"
    
    local response
    local status_code
    
    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            ${headers:+-H "$headers"} \
            -d "$data" \
            "$BASE_URL$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            ${headers:+-H "$headers"} \
            "$BASE_URL$endpoint")
    fi
    
    status_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | head -n -1)
    
    if [ "$status_code" = "$expected_status" ]; then
        log "✅ $description - Status: $status_code" "$GREEN"
        echo "$response_body" | jq . 2>/dev/null || echo "$response_body"
        return 0
    else
        log "❌ $description - Expected: $expected_status, Got: $status_code" "$RED"
        echo "Response: $response_body"
        return 1
    fi
}

# Check if server is running
check_server() {
    log "Checking if server is running..." "$YELLOW"
    if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        log "✅ Server is running" "$GREEN"
        return 0
    else
        log "❌ Server is not running. Please start the Java backend first." "$RED"
        exit 1
    fi
}

# Test database connectivity
test_database_connectivity() {
    log "Testing database connectivity..." "$YELLOW"
    test_endpoint "Database Health Check" "GET" "/actuator/health" "" "" "200"
}

# Test admin login
test_admin_login() {
    log "Testing admin login..." "$YELLOW"
    
    local login_data="{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}"
    local response
    
    response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "$login_data" \
        "$BASE_URL/api/auth/login")
    
    local status_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)
    
    if [ "$status_code" = "200" ]; then
        JWT_TOKEN=$(echo "$response_body" | jq -r '.token // .accessToken // .access_token' 2>/dev/null)
        if [ "$JWT_TOKEN" != "null" ] && [ -n "$JWT_TOKEN" ]; then
            log "✅ Admin login successful" "$GREEN"
            log "JWT Token: ${JWT_TOKEN:0:50}..." "$GREEN"
            return 0
        else
            log "❌ Login response doesn't contain valid token" "$RED"
            echo "Response: $response_body"
            return 1
        fi
    else
        log "❌ Admin login failed - Status: $status_code" "$RED"
        echo "Response: $response_body"
        return 1
    fi
}

# Test admin product fetching
test_admin_get_products() {
    if [ -z "$JWT_TOKEN" ]; then
        log "❌ No JWT token available for admin product test" "$RED"
        return 1
    fi
    
    log "Testing admin product fetching..." "$YELLOW"
    test_endpoint "Get All Products (Admin)" "GET" "/api/products" "Authorization: Bearer $JWT_TOKEN" "" "200"
}

# Test admin product creation
test_admin_create_product() {
    if [ -z "$JWT_TOKEN" ]; then
        log "❌ No JWT token available for admin product creation test" "$RED"
        return 1
    fi
    
    log "Testing admin product creation..." "$YELLOW"
    local product_data='{
        "name": "Test Product",
        "description": "A test product created by automation",
        "price": 19.99,
        "stock": 100,
        "imageUrl": "https://example.com/test-product.jpg"
    }'
    
    test_endpoint "Create Product (Admin)" "POST" "/api/products" "Authorization: Bearer $JWT_TOKEN" "$product_data" "201"
}

# Test admin user management
test_admin_get_users() {
    if [ -z "$JWT_TOKEN" ]; then
        log "❌ No JWT token available for admin users test" "$RED"
        return 1
    fi
    
    log "Testing admin user management..." "$YELLOW"
    test_endpoint "Get All Users (Admin)" "GET" "/api/admin/users" "Authorization: Bearer $JWT_TOKEN" "" "200"
}

# Test admin order management
test_admin_get_orders() {
    if [ -z "$JWT_TOKEN" ]; then
        log "❌ No JWT token available for admin orders test" "$RED"
        return 1
    fi
    
    log "Testing admin order management..." "$YELLOW"
    test_endpoint "Get All Orders (Admin)" "GET" "/api/admin/orders" "Authorization: Bearer $JWT_TOKEN" "" "200"
}

# Test database sync functionality
test_database_sync() {
    if [ -z "$JWT_TOKEN" ]; then
        log "❌ No JWT token available for database sync test" "$RED"
        return 1
    fi
    
    log "Testing database sync functionality..." "$YELLOW"
    test_endpoint "Database Sync Status" "GET" "/api/admin/database-sync/status" "Authorization: Bearer $JWT_TOKEN" "" "200"
}

# Main test execution
main() {
    log "🚀 Starting Admin Workflow Test Suite" "$GREEN"
    log "Base URL: $BASE_URL" "$YELLOW"
    log "Admin Email: $ADMIN_EMAIL" "$YELLOW"
    log "Results will be saved to: $TEST_RESULTS_FILE" "$YELLOW"
    
    # Initialize test counters
    local total_tests=0
    local passed_tests=0
    
    # Test server availability
    check_server
    
    # Test database connectivity
    ((total_tests++))
    if test_database_connectivity; then
        ((passed_tests++))
    fi
    
    # Test admin login
    ((total_tests++))
    if test_admin_login; then
        ((passed_tests++))
    fi
    
    # Test admin product operations
    ((total_tests++))
    if test_admin_get_products; then
        ((passed_tests++))
    fi
    
    ((total_tests++))
    if test_admin_create_product; then
        ((passed_tests++))
    fi
    
    # Test admin user management
    ((total_tests++))
    if test_admin_get_users; then
        ((passed_tests++))
    fi
    
    # Test admin order management
    ((total_tests++))
    if test_admin_get_orders; then
        ((passed_tests++))
    fi
    
    # Test database sync
    ((total_tests++))
    if test_database_sync; then
        ((passed_tests++))
    fi
    
    # Print summary
    log "📊 Test Summary:" "$YELLOW"
    log "Total Tests: $total_tests" "$YELLOW"
    log "Passed: $passed_tests" "$GREEN"
    log "Failed: $((total_tests - passed_tests))" "$RED"
    
    if [ $passed_tests -eq $total_tests ]; then
        log "🎉 All tests passed!" "$GREEN"
        exit 0
    else
        log "⚠️  Some tests failed. Check the logs above for details." "$RED"
        exit 1
    fi
}

# Check dependencies
if ! command -v curl &> /dev/null; then
    log "❌ curl is required but not installed" "$RED"
    exit 1
fi

if ! command -v jq &> /dev/null; then
    log "⚠️  jq is not installed. JSON responses won't be formatted nicely" "$YELLOW"
fi

# Run main function
main "$@"