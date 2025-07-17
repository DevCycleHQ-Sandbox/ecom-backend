#!/bin/bash

# Database Connectivity Test Script
# Specifically tests Neon DB connection and common database issues

set -e

# Configuration
BASE_URL="http://localhost:8080"
LOG_FILE="database_test_$(date +%Y%m%d_%H%M%S).log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${2:-$NC}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}" | tee -a "$LOG_FILE"
}

# Function to test basic database connectivity
test_basic_db_connection() {
    log "Testing basic database connectivity..." "$YELLOW"
    
    local response
    local status_code
    
    response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/actuator/health" 2>/dev/null || echo "Connection failed")
    
    if [[ "$response" == *"Connection failed"* ]]; then
        log "❌ Cannot connect to application server" "$RED"
        return 1
    fi
    
    status_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | head -n -1)
    
    if [ "$status_code" = "200" ]; then
        log "✅ Application server is responding" "$GREEN"
        
        # Check if database status is in the health response
        if echo "$response_body" | grep -q "database\|db\|datasource"; then
            log "Database status found in health check:" "$BLUE"
            echo "$response_body" | jq '.components.db // .components.database // .components.datasource // .' 2>/dev/null || echo "$response_body"
        fi
        return 0
    else
        log "❌ Application server returned status: $status_code" "$RED"
        echo "Response: $response_body"
        return 1
    fi
}

# Function to test database-specific endpoints
test_database_endpoints() {
    log "Testing database-specific endpoints..." "$YELLOW"
    
    local endpoints=(
        "/actuator/health/db"
        "/actuator/health/datasource"
        "/api/health/database"
        "/api/admin/database-sync/status"
    )
    
    for endpoint in "${endpoints[@]}"; do
        log "Testing endpoint: $endpoint" "$BLUE"
        
        local response
        local status_code
        
        response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL$endpoint" 2>/dev/null || echo -e "\nConnection failed")
        status_code=$(echo "$response" | tail -n1)
        response_body=$(echo "$response" | head -n -1)
        
        if [ "$status_code" = "200" ]; then
            log "✅ $endpoint is responding" "$GREEN"
            echo "$response_body" | jq . 2>/dev/null || echo "$response_body"
        elif [ "$status_code" = "401" ] || [ "$status_code" = "403" ]; then
            log "⚠️  $endpoint requires authentication (Status: $status_code)" "$YELLOW"
        else
            log "❌ $endpoint failed with status: $status_code" "$RED"
        fi
        echo ""
    done
}

# Function to test database operations through API
test_database_operations() {
    log "Testing database operations through API..." "$YELLOW"
    
    # Test a simple read operation that doesn't require auth
    log "Testing product listing (should work without auth)..." "$BLUE"
    local response
    local status_code
    
    response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/products" 2>/dev/null || echo -e "\nConnection failed")
    status_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | head -n -1)
    
    if [ "$status_code" = "200" ]; then
        log "✅ Product listing is working" "$GREEN"
        local product_count=$(echo "$response_body" | jq 'length // 0' 2>/dev/null || echo "unknown")
        log "Products found: $product_count" "$GREEN"
    else
        log "❌ Product listing failed with status: $status_code" "$RED"
        echo "Response: $response_body"
        
        # Check for common database error patterns
        if echo "$response_body" | grep -i "connection.*refused\|timeout\|database.*error\|sql.*exception"; then
            log "🔍 Database connection error detected!" "$RED"
        elif echo "$response_body" | grep -i "neon\|postgres"; then
            log "🔍 PostgreSQL/Neon specific error detected!" "$RED"
        fi
    fi
}

# Function to check application logs for database errors
check_application_logs() {
    log "Checking application logs for database errors..." "$YELLOW"
    
    local log_files=(
        "app.log"
        "application.log"
        "spring.log"
        "../logs/application.log"
        "logs/application.log"
    )
    
    for log_file in "${log_files[@]}"; do
        if [ -f "$log_file" ]; then
            log "Found log file: $log_file" "$BLUE"
            
            # Look for database-related errors in the last 100 lines
            local db_errors
            db_errors=$(tail -n 100 "$log_file" | grep -i "error\|exception\|failed" | grep -i "database\|connection\|sql\|neon\|postgres" | tail -n 10)
            
            if [ -n "$db_errors" ]; then
                log "🔍 Database errors found in $log_file:" "$RED"
                echo "$db_errors"
            else
                log "✅ No recent database errors in $log_file" "$GREEN"
            fi
            echo ""
        fi
    done
}

# Function to test Neon DB specific connectivity
test_neon_connectivity() {
    log "Testing Neon DB specific connectivity..." "$YELLOW"
    
    # Check if we can find Neon connection strings in configuration
    local config_files=(
        "src/main/resources/application.yml"
        "src/main/resources/application.properties"
        "src/main/resources/application-production.yml"
        ".env"
        "../.env"
    )
    
    for config_file in "${config_files[@]}"; do
        if [ -f "$config_file" ]; then
            log "Checking configuration file: $config_file" "$BLUE"
            
            # Look for Neon-specific configuration
            if grep -i "neon\|\.neon\.tech" "$config_file" > /dev/null 2>&1; then
                log "✅ Neon configuration found in $config_file" "$GREEN"
                
                # Extract and show connection details (masked)
                local neon_urls
                neon_urls=$(grep -i "neon\|\.neon\.tech" "$config_file" | sed 's/password=[^@&]*/password=****/g')
                echo "$neon_urls"
            else
                log "⚠️  No Neon configuration found in $config_file" "$YELLOW"
            fi
            echo ""
        fi
    done
}

# Function to provide diagnostic recommendations
provide_recommendations() {
    log "🔧 Diagnostic Recommendations:" "$YELLOW"
    
    echo ""
    log "Common Neon DB Issues and Solutions:" "$BLUE"
    
    echo "1. Connection Timeout Issues:"
    echo "   - Check if Neon DB is in sleep mode (common with free tier)"
    echo "   - Verify connection pool settings in application.yml"
    echo "   - Increase connection timeout values"
    echo ""
    
    echo "2. Authentication Issues:"
    echo "   - Verify username/password in environment variables"
    echo "   - Check if database credentials have expired"
    echo "   - Ensure connection string includes correct parameters"
    echo ""
    
    echo "3. Network Issues:"
    echo "   - Verify firewall rules allow outbound connections to Neon"
    echo "   - Check if your server's IP is whitelisted (if applicable)"
    echo "   - Test direct connection with psql if available"
    echo ""
    
    echo "4. Configuration Issues:"
    echo "   - Ensure SSL mode is set correctly (usually 'require' for Neon)"
    echo "   - Verify database name, schema, and table configurations"
    echo "   - Check dual database configuration if using sync features"
    echo ""
    
    log "Recommended Commands to Run:" "$BLUE"
    echo "1. Check application properties:"
    echo "   grep -r \"neon\\|postgres\" src/main/resources/"
    echo ""
    echo "2. Test direct connection (if psql is available):"
    echo "   psql \$DATABASE_URL"
    echo ""
    echo "3. Check recent application logs:"
    echo "   tail -f app.log | grep -i \"error\\|exception\""
    echo ""
    echo "4. Restart application with debug logging:"
    echo "   java -jar target/*.jar --logging.level.org.springframework.jdbc=DEBUG"
    echo ""
}

# Main execution function
main() {
    log "🚀 Starting Database Connectivity Test Suite" "$GREEN"
    log "Base URL: $BASE_URL" "$YELLOW"
    log "Results will be saved to: $LOG_FILE" "$YELLOW"
    echo ""
    
    # Test basic connectivity
    if ! test_basic_db_connection; then
        log "⚠️  Basic connectivity failed. Skipping detailed tests." "$YELLOW"
        provide_recommendations
        exit 1
    fi
    echo ""
    
    # Test database-specific endpoints
    test_database_endpoints
    echo ""
    
    # Test database operations
    test_database_operations
    echo ""
    
    # Check application logs
    check_application_logs
    echo ""
    
    # Test Neon-specific connectivity
    test_neon_connectivity
    echo ""
    
    # Provide recommendations
    provide_recommendations
    
    log "✅ Database connectivity test completed. Check $LOG_FILE for detailed results." "$GREEN"
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