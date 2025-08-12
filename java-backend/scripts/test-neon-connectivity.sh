#!/bin/bash

# Neon Database Connectivity Test Script
# Tests the use-neon feature flag and database routing

set -e  # Exit on any error

BASE_URL="http://localhost:8080"
ADMIN_USERNAME="admin"
ADMIN_PASSWORD="admin123"

echo "🌐 Testing Neon Database Connectivity..."
echo "======================================="

# Function to make HTTP requests
make_request() {
    local method=$1
    local url=$2
    local data=$3
    local headers=$4
    
    echo "📡 Making $method request to: $url"
    
    if [ -n "$data" ]; then
        if [ -n "$headers" ]; then
            curl -s -X "$method" "$url" \
                -H "Content-Type: application/json" \
                -H "$headers" \
                -d "$data" \
                --write-out "\nHTTP_CODE:%{http_code}\n" \
                --max-time 30
        else
            curl -s -X "$method" "$url" \
                -H "Content-Type: application/json" \
                -d "$data" \
                --write-out "\nHTTP_CODE:%{http_code}\n" \
                --max-time 30
        fi
    else
        if [ -n "$headers" ]; then
            curl -s -X "$method" "$url" \
                -H "$headers" \
                --write-out "\nHTTP_CODE:%{http_code}\n" \
                --max-time 30
        else
            curl -s -X "$method" "$url" \
                --write-out "\nHTTP_CODE:%{http_code}\n" \
                --max-time 30
        fi
    fi
}

# Function to extract HTTP code from response
extract_http_code() {
    echo "$1" | grep "HTTP_CODE:" | cut -d: -f2
}

# Function to extract JSON response without HTTP code
extract_json() {
    echo "$1" | sed '/HTTP_CODE:/d'
}

# Check environment variables
echo "🔍 Checking Environment Variables:"
echo "NEON_DATABASE_URL: ${NEON_DATABASE_URL:-'Not set'}"
echo "DEVCYCLE_SERVER_SDK_KEY: ${DEVCYCLE_SERVER_SDK_KEY:-'Not set'}"
echo ""

# Step 1: Get admin JWT token
echo "🔐 Step 1: Admin Login"
admin_login_data='{
    "username": "'$ADMIN_USERNAME'",
    "password": "'$ADMIN_PASSWORD'"
}'

login_response=$(make_request "POST" "$BASE_URL/auth/login" "$admin_login_data")
login_code=$(extract_http_code "$login_response")

if [ "$login_code" = "200" ]; then
    echo "✅ Admin login successful"
    jwt_token=$(extract_json "$login_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    echo "🔑 JWT Token: ${jwt_token:0:20}..."
else
    echo "❌ Admin login failed with code: $login_code"
    echo "Response: $(extract_json "$login_response")"
    exit 1
fi

# Step 2: Check current feature flag status
echo ""
echo "🎛️  Step 2: Check Feature Flag Status"
flag_response=$(make_request "GET" "$BASE_URL/api/products/with-feature-flag" "" "Authorization: Bearer $jwt_token")
flag_code=$(extract_http_code "$flag_response")

if [ "$flag_code" = "200" ]; then
    echo "✅ Feature flag endpoint accessible"
    flag_info=$(extract_json "$flag_response")
    echo "🎛️  Current Flags: $flag_info"
    
    # Extract use-neon flag value
    use_neon=$(echo "$flag_info" | grep -o '"use-neon":[^,}]*' | cut -d: -f2 | tr -d ' ')
    echo "🌐 use-neon flag: $use_neon"
else
    echo "❌ Feature flag endpoint failed with code: $flag_code"
    echo "Response: $(extract_json "$flag_response")"
fi

# Step 3: Test database debug endpoint
echo ""
echo "🔧 Step 3: Test Database Debug Endpoint"
debug_response=$(make_request "GET" "$BASE_URL/api/products/debug/cart-items" "" "Authorization: Bearer $jwt_token")
debug_code=$(extract_http_code "$debug_response")

if [ "$debug_code" = "200" ]; then
    echo "✅ Database debug endpoint working"
    debug_info=$(extract_json "$debug_response")
    echo "🔧 Debug Info: $debug_info"
    
    # Extract database counts
    primary_count=$(echo "$debug_info" | grep -o '"primary_count":[^,}]*' | cut -d: -f2 | tr -d ' ')
    secondary_count=$(echo "$debug_info" | grep -o '"secondary_count":[^,}]*' | cut -d: -f2 | tr -d ' "')
    
    echo "📊 Primary DB Count: $primary_count"
    echo "📊 Secondary DB Count: $secondary_count"
else
    echo "⚠️  Database debug endpoint failed with code: $debug_code"
    echo "Response: $(extract_json "$debug_response")"
fi

# Step 4: Test product fetching with different scenarios
echo ""
echo "📋 Step 4: Test Product Fetching"
products_response=$(make_request "GET" "$BASE_URL/api/products" "" "Authorization: Bearer $jwt_token")
products_code=$(extract_http_code "$products_response")

if [ "$products_code" = "200" ]; then
    echo "✅ Products fetched successfully"
    products_list=$(extract_json "$products_response")
    product_count=$(echo "$products_list" | grep -o '"id":' | wc -l)
    echo "📊 Found $product_count products"
else
    echo "❌ Products fetch failed with code: $products_code"
    echo "Response: $(extract_json "$products_response")"
    echo ""
    echo "🔍 Debugging Tips:"
    echo "1. Check if SQLite database file exists and has data"
    echo "2. Verify Neon database connection if use-neon flag is enabled"
    echo "3. Check application logs for database connection errors"
    echo "4. Ensure feature flag service is working correctly"
fi

# Step 5: Health check for database connectivity
echo ""
echo "🏥 Step 5: Extended Health Check"
health_response=$(make_request "GET" "$BASE_URL/api/health/debug/cart-items" "" "Authorization: Bearer $jwt_token")
health_code=$(extract_http_code "$health_response")

if [ "$health_code" = "200" ]; then
    echo "✅ Extended health check passed"
    health_info=$(extract_json "$health_response")
    echo "🏥 Health Info: $health_info"
else
    echo "⚠️  Extended health check failed with code: $health_code"
    echo "Response: $(extract_json "$health_response")"
fi

# Step 6: Recommendations based on results
echo ""
echo "📋 Step 6: Analysis and Recommendations"
echo "======================================="

if [ -z "${NEON_DATABASE_URL:-}" ]; then
    echo "⚠️  NEON_DATABASE_URL is not set"
    echo "🔧 Recommendation: Set up Neon database connection or disable secondary database"
fi

if [ -z "${DEVCYCLE_SERVER_SDK_KEY:-}" ] || [ "${DEVCYCLE_SERVER_SDK_KEY:-}" = "your-devcycle-server-sdk-key" ]; then
    echo "⚠️  DevCycle SDK key is not properly configured"
    echo "🔧 Recommendation: Feature flags will use fallback values"
fi

if [ "$products_code" != "200" ]; then
    echo "❌ CRITICAL ISSUE: Product fetching is failing"
    echo ""
    echo "🔍 Troubleshooting Steps:"
    echo "1. Check if primary database (SQLite) is accessible:"
    echo "   - File: database.sqlite should exist"
    echo "   - Permissions: Should be readable/writable"
    echo ""
    echo "2. If use-neon flag is enabled, check Neon connectivity:"
    echo "   - NEON_DATABASE_URL should be set"
    echo "   - Network connectivity to Neon"
    echo "   - Database credentials"
    echo ""
    echo "3. Check application logs for specific errors:"
    echo "   - Database connection errors"
    echo "   - SQL execution errors"
    echo "   - Feature flag evaluation errors"
    echo ""
    echo "4. Verify configuration:"
    echo "   - application.properties has correct database settings"
    echo "   - Spring profiles are set correctly"
    exit 1
else
    echo "✅ Products are fetching successfully!"
    echo "🎯 Database connectivity appears to be working"
fi

echo ""
echo "🎉 Neon Connectivity Test Completed!" 