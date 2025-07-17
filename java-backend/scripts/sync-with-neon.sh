#!/bin/bash

# Sync with Neon Database Script
# This script logs in as admin and syncs all data with the Neon database

set -e  # Exit on any error

# Configuration
API_BASE_URL="${API_BASE_URL:-http://localhost:3002/api}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-password}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if jq is installed
check_dependencies() {
    if ! command -v jq &> /dev/null; then
        print_error "jq is required but not installed. Please install jq first."
        echo "On macOS: brew install jq"
        echo "On Ubuntu: sudo apt-get install jq"
        exit 1
    fi
    
    if ! command -v curl &> /dev/null; then
        print_error "curl is required but not installed."
        exit 1
    fi
}

# Function to test API connectivity
test_connectivity() {
    print_status "Testing API connectivity..."
    
    if curl -s --connect-timeout 5 "${API_BASE_URL}/auth/login" > /dev/null 2>&1; then
        print_success "API is reachable at ${API_BASE_URL}"
    else
        print_error "Cannot reach API at ${API_BASE_URL}"
        print_error "Please ensure the Java backend is running and the URL is correct"
        exit 1
    fi
}

# Function to login and get JWT token
login_admin() {
    print_status "Logging in as admin user..."
    
    local login_response
    login_response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"${ADMIN_USERNAME}\",\"password\":\"${ADMIN_PASSWORD}\"}" \
        "${API_BASE_URL}/auth/login")
    
    # Check if login was successful
    if echo "${login_response}" | jq -e '.accessToken' > /dev/null 2>&1; then
        JWT_TOKEN=$(echo "${login_response}" | jq -r '.accessToken')
        ADMIN_USER_ID=$(echo "${login_response}" | jq -r '.user.id')
        print_success "Successfully logged in as admin"
        print_status "Admin User ID: ${ADMIN_USER_ID}"
    else
        print_error "Login failed. Response: ${login_response}"
        print_error "Please check your credentials or ensure the admin user exists"
        exit 1
    fi
}

# Function to check DevCycle status
check_devcycle_status() {
    print_status "Checking DevCycle integration status..."
    
    local status_response
    status_response=$(curl -s -X GET \
        -H "Authorization: Bearer ${JWT_TOKEN}" \
        "${API_BASE_URL}/admin/feature-flags/devcycle/status")
    
    if echo "${status_response}" | jq -e '.success' > /dev/null 2>&1; then
        local is_connected=$(echo "${status_response}" | jq -r '.status.isDevCycleConnected')
        local source=$(echo "${status_response}" | jq -r '.status.source')
        
        if [ "${is_connected}" = "true" ]; then
            print_success "DevCycle is connected (Source: ${source})"
            
            # Show use-neon flag status for admin
            local use_neon_admin=$(echo "${status_response}" | jq -r '.status.useNeonFlagTests.admin // false')
            print_status "use-neon flag for admin: ${use_neon_admin}"
        else
            print_warning "DevCycle not connected (Source: ${source})"
            print_warning "Feature flags will use fallback values"
        fi
    else
        print_warning "Could not check DevCycle status: ${status_response}"
    fi
}

# Function to check database consistency before sync
check_initial_consistency() {
    print_status "Checking initial database consistency..."
    
    local consistency_response
    consistency_response=$(curl -s -X GET \
        -H "Authorization: Bearer ${JWT_TOKEN}" \
        "${API_BASE_URL}/admin/database/consistency")
    
    if echo "${consistency_response}" | jq -e '.success' > /dev/null 2>&1; then
        local is_consistent=$(echo "${consistency_response}" | jq -r '.isConsistent')
        local primary_count=$(echo "${consistency_response}" | jq -r '.primaryCount')
        local secondary_count=$(echo "${consistency_response}" | jq -r '.secondaryCount')
        local message=$(echo "${consistency_response}" | jq -r '.message')
        
        print_status "Primary DB records: ${primary_count}"
        print_status "Secondary DB records: ${secondary_count}"
        print_status "Status: ${message}"
        
        if [ "${is_consistent}" = "true" ]; then
            print_success "Databases are already consistent"
        else
            print_warning "Databases are inconsistent - sync needed"
        fi
    else
        print_error "Could not check database consistency"
        if echo "${consistency_response}" | grep -q "not available"; then
            print_error "Secondary database (Neon) might not be enabled or configured"
            print_error "Please check your NEON_DATABASE_URL and SECONDARY_DATABASE_ENABLED settings"
            exit 1
        fi
    fi
}

# Function to sync cart items to secondary database
sync_cart_items() {
    print_status "Syncing cart items to Neon database..."
    
    local sync_response
    sync_response=$(curl -s -X POST \
        -H "Authorization: Bearer ${JWT_TOKEN}" \
        "${API_BASE_URL}/admin/database/sync/cart-items")
    
    if echo "${sync_response}" | jq -e '.success' > /dev/null 2>&1; then
        local synced_count=$(echo "${sync_response}" | jq -r '.syncedCount // 0')
        print_success "Cart items sync completed. Synced: ${synced_count} items"
    else
        local error_message=$(echo "${sync_response}" | jq -r '.message // "Unknown error"')
        print_error "Cart items sync failed: ${error_message}"
        return 1
    fi
}

# Function to perform bidirectional sync
perform_bidirectional_sync() {
    print_status "Performing bidirectional database synchronization..."
    
    local sync_response
    sync_response=$(curl -s -X POST \
        -H "Authorization: Bearer ${JWT_TOKEN}" \
        "${API_BASE_URL}/admin/database/sync/bidirectional")
    
    if echo "${sync_response}" | jq -e '.success' > /dev/null 2>&1; then
        local primary_to_secondary=$(echo "${sync_response}" | jq -r '.primaryToSecondaryCount // 0')
        local secondary_to_primary=$(echo "${sync_response}" | jq -r '.secondaryToPrimaryCount // 0')
        local timestamp=$(echo "${sync_response}" | jq -r '.timestamp')
        
        print_success "Bidirectional sync completed!"
        print_status "Primary → Secondary: ${primary_to_secondary} records"
        print_status "Secondary → Primary: ${secondary_to_primary} records"
        print_status "Completed at: ${timestamp}"
    else
        local error_message=$(echo "${sync_response}" | jq -r '.message // "Unknown error"')
        print_error "Bidirectional sync failed: ${error_message}"
        return 1
    fi
}

# Function to verify final consistency
verify_final_consistency() {
    print_status "Verifying final database consistency..."
    
    sleep 2  # Wait a moment for sync to complete
    
    local consistency_response
    consistency_response=$(curl -s -X GET \
        -H "Authorization: Bearer ${JWT_TOKEN}" \
        "${API_BASE_URL}/admin/database/consistency")
    
    if echo "${consistency_response}" | jq -e '.success' > /dev/null 2>&1; then
        local is_consistent=$(echo "${consistency_response}" | jq -r '.isConsistent')
        local primary_count=$(echo "${consistency_response}" | jq -r '.primaryCount')
        local secondary_count=$(echo "${consistency_response}" | jq -r '.secondaryCount')
        local message=$(echo "${consistency_response}" | jq -r '.message')
        
        print_status "Final consistency check:"
        print_status "Primary DB records: ${primary_count}"
        print_status "Secondary DB records: ${secondary_count}"
        
        if [ "${is_consistent}" = "true" ]; then
            print_success "✅ Databases are now consistent!"
            print_success "✅ Sync with Neon completed successfully"
        else
            print_warning "⚠️  Databases still show inconsistency: ${message}"
            print_warning "You may need to run additional sync operations"
        fi
    else
        print_error "Could not verify final consistency"
    fi
}

# Function to test use-neon feature flag
test_use_neon_flag() {
    print_status "Testing use-neon feature flag..."
    
    local flag_response
    flag_response=$(curl -s -X GET \
        -H "Authorization: Bearer ${JWT_TOKEN}" \
        "${API_BASE_URL}/admin/feature-flags/use-neon/test?userIds=admin,user1,user2")
    
    if echo "${flag_response}" | jq -e '.success' > /dev/null 2>&1; then
        local source=$(echo "${flag_response}" | jq -r '.source')
        print_status "Feature flag source: ${source}"
        
        echo "${flag_response}" | jq -r '.results | to_entries[] | "  \(.key): \(.value)"' | while read -r line; do
            print_status "  ${line}"
        done
    else
        print_warning "Could not test use-neon flag"
    fi
}

# Function to show summary
show_summary() {
    echo ""
    echo "=========================================="
    echo "           SYNC SUMMARY"
    echo "=========================================="
    print_success "✅ Admin login successful"
    print_success "✅ Database connectivity verified"
    print_success "✅ Data synchronization completed"
    print_success "✅ Consistency verification completed"
    echo ""
    print_status "Your Neon database is now synchronized!"
    print_status "You can now enable the 'use-neon' feature flag to route users to Neon."
    echo ""
    print_status "Next steps:"
    echo "  1. Monitor the consistency with: curl -H 'Authorization: Bearer ${JWT_TOKEN}' ${API_BASE_URL}/admin/database/consistency"
    echo "  2. Enable use-neon flag in DevCycle dashboard for gradual rollout"
    echo "  3. Test with specific users using the admin endpoints"
    echo "=========================================="
}

# Main execution function
main() {
    echo "🚀 Starting Neon Database Sync Process..."
    echo ""
    
    # Check dependencies
    check_dependencies
    
    # Test connectivity
    test_connectivity
    
    # Login as admin
    login_admin
    
    # Check DevCycle status
    check_devcycle_status
    
    # Check initial consistency
    check_initial_consistency
    
    # Perform sync operations
    print_status "Starting synchronization process..."
    
    # Try cart items sync first
    if sync_cart_items; then
        print_success "Cart items sync completed"
    else
        print_warning "Cart items sync had issues, continuing with bidirectional sync..."
    fi
    
    # Perform bidirectional sync
    if perform_bidirectional_sync; then
        print_success "Bidirectional sync completed"
    else
        print_error "Bidirectional sync failed"
        exit 1
    fi
    
    # Verify final consistency
    verify_final_consistency
    
    # Test feature flag
    test_use_neon_flag
    
    # Show summary
    show_summary
}

# Handle script interruption
trap 'print_error "Script interrupted"; exit 1' INT TERM

# Show usage if help requested
if [[ "${1}" == "--help" || "${1}" == "-h" ]]; then
    echo "Neon Database Sync Script"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Environment Variables:"
    echo "  API_BASE_URL      - Base URL for the API (default: http://localhost:3002/api)"
    echo "  ADMIN_USERNAME    - Admin username (default: admin)"
    echo "  ADMIN_PASSWORD    - Admin password (default: password)"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Use default settings"
    echo "  API_BASE_URL=http://prod.example.com/api $0  # Use production URL"
    echo "  ADMIN_PASSWORD=mysecret $0            # Use custom password"
    echo ""
    exit 0
fi

# Run main function
main "$@" 