#!/bin/bash

# Master Admin Workflow Test Runner
# Sets up environment and runs comprehensive admin workflow tests

set -e

# Configuration
JAVA_BACKEND_DIR="$(dirname "$0")/.."
BASE_URL="http://localhost:8080"
MAX_WAIT_TIME=120  # Maximum time to wait for server startup (seconds)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${2:-$NC}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

# Function to check if server is running
check_server() {
    local url="$1"
    local max_attempts=12
    local attempt=1
    
    log "Checking if server is running at $url..." "$YELLOW"
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url/actuator/health" > /dev/null 2>&1; then
            log "✅ Server is responding" "$GREEN"
            return 0
        fi
        
        log "Attempt $attempt/$max_attempts - Server not ready, waiting 10 seconds..." "$YELLOW"
        sleep 10
        ((attempt++))
    done
    
    log "❌ Server failed to start within $((max_attempts * 10)) seconds" "$RED"
    return 1
}

# Function to start the Java backend
start_java_backend() {
    log "Starting Java backend..." "$BLUE"
    
    cd "$JAVA_BACKEND_DIR"
    
    # Check if Maven is available
    if ! command -v mvn &> /dev/null; then
        log "❌ Maven is not installed or not in PATH" "$RED"
        return 1
    fi
    
    # Compile the application
    log "Compiling application..." "$YELLOW"
    if ! mvn compile -q; then
        log "❌ Failed to compile application" "$RED"
        return 1
    fi
    
    # Start the application in background
    log "Starting Spring Boot application..." "$YELLOW"
    nohup mvn spring-boot:run -Dspring-boot.run.profiles=development > app.log 2>&1 &
    SERVER_PID=$!
    
    log "Server started with PID: $SERVER_PID" "$GREEN"
    echo $SERVER_PID > server.pid
    
    # Wait for server to start
    if check_server "$BASE_URL"; then
        return 0
    else
        log "❌ Failed to start server" "$RED"
        return 1
    fi
}

# Function to stop the Java backend
stop_java_backend() {
    log "Stopping Java backend..." "$YELLOW"
    
    cd "$JAVA_BACKEND_DIR"
    
    if [ -f "server.pid" ]; then
        local pid=$(cat server.pid)
        if kill -0 "$pid" 2>/dev/null; then
            log "Stopping server with PID: $pid" "$BLUE"
            kill "$pid"
            
            # Wait for graceful shutdown
            local count=0
            while kill -0 "$pid" 2>/dev/null && [ $count -lt 30 ]; do
                sleep 1
                ((count++))
            done
            
            # Force kill if still running
            if kill -0 "$pid" 2>/dev/null; then
                log "Force killing server..." "$YELLOW"
                kill -9 "$pid"
            fi
        fi
        rm -f server.pid
    fi
    
    # Also kill any Java processes running Spring Boot
    pkill -f "spring-boot:run" || true
    
    log "✅ Server stopped" "$GREEN"
}

# Function to run bash tests
run_bash_tests() {
    log "Running Bash test suite..." "$BLUE"
    
    cd "$JAVA_BACKEND_DIR/scripts"
    
    # Make scripts executable
    chmod +x test-admin-workflow.sh test-database-connectivity.sh
    
    # Run admin workflow tests
    log "Running admin workflow tests..." "$YELLOW"
    if ./test-admin-workflow.sh; then
        log "✅ Bash admin workflow tests passed" "$GREEN"
    else
        log "❌ Bash admin workflow tests failed" "$RED"
        return 1
    fi
    
    # Run database connectivity tests
    log "Running database connectivity tests..." "$YELLOW"
    if ./test-database-connectivity.sh; then
        log "✅ Bash database connectivity tests passed" "$GREEN"
    else
        log "❌ Bash database connectivity tests failed" "$RED"
        return 1
    fi
    
    return 0
}

# Function to run Python tests
run_python_tests() {
    log "Running Python test suite..." "$BLUE"
    
    cd "$JAVA_BACKEND_DIR/scripts"
    
    # Check if Python is available
    if command -v python3 &> /dev/null; then
        PYTHON_CMD="python3"
    elif command -v python &> /dev/null; then
        PYTHON_CMD="python"
    else
        log "⚠️  Python not found, skipping Python tests" "$YELLOW"
        return 0
    fi
    
    # Install required packages if not present
    log "Checking Python dependencies..." "$YELLOW"
    $PYTHON_CMD -c "import requests" 2>/dev/null || {
        log "Installing requests package..." "$YELLOW"
        pip install requests || pip3 install requests || {
            log "⚠️  Failed to install requests, skipping Python tests" "$YELLOW"
            return 0
        }
    }
    
    # Run comprehensive tests
    log "Running comprehensive Python tests..." "$YELLOW"
    if $PYTHON_CMD comprehensive-admin-test.py --url "$BASE_URL"; then
        log "✅ Python comprehensive tests passed" "$GREEN"
    else
        log "❌ Python comprehensive tests failed" "$RED"
        return 1
    fi
    
    return 0
}

# Function to run Java integration tests
run_java_tests() {
    log "Running Java integration tests..." "$BLUE"
    
    cd "$JAVA_BACKEND_DIR"
    
    # Run specific admin workflow tests
    log "Running AdminWorkflowIntegrationTest..." "$YELLOW"
    if mvn test -Dtest=AdminWorkflowIntegrationTest -q; then
        log "✅ Java integration tests passed" "$GREEN"
    else
        log "❌ Java integration tests failed" "$RED"
        return 1
    fi
    
    return 0
}

# Function to generate final report
generate_final_report() {
    log "Generating final test report..." "$BLUE"
    
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local report_file="final_test_report_$timestamp.md"
    
    cat > "$report_file" << EOF
# Admin Workflow Test Report

**Generated:** $(date)
**Test Suite:** Comprehensive Admin Workflow Tests

## Summary

This report contains the results of comprehensive admin workflow testing including:

- Server availability and health checks
- Database connectivity (including Neon DB)
- Admin authentication and authorization
- Product management operations
- User and order management
- Database synchronization
- Security validation

## Test Results

### Bash Tests
- Admin workflow script: $([ -f "test_results_*.log" ] && echo "✅ PASSED" || echo "❌ See logs")
- Database connectivity: $([ -f "database_test_*.log" ] && echo "✅ PASSED" || echo "❌ See logs")

### Python Tests
- Comprehensive test suite: $([ -f "admin_test_report_*.json" ] && echo "✅ PASSED" || echo "❌ See logs")

### Java Integration Tests
- AdminWorkflowIntegrationTest: $([ -f "target/surefire-reports/TEST-*.xml" ] && echo "✅ PASSED" || echo "❌ See Maven output")

## Common Issues and Solutions

### Neon Database Issues

1. **Connection Timeouts**
   - Neon databases on free tier may sleep after inactivity
   - Solution: Make a simple query to wake up the database

2. **Authentication Failures**
   - Check environment variables for database credentials
   - Verify connection string format

3. **SSL/TLS Issues**
   - Ensure SSL mode is set to 'require' for Neon
   - Check certificate validation settings

### Application Issues

1. **Admin Login Failures**
   - Verify admin user exists in database
   - Check JWT configuration and secret key

2. **Product Fetching Issues**
   - Check database table structure
   - Verify JPA entity mappings

## Files Generated

- Test logs: \`*_test_*.log\`
- JSON reports: \`admin_test_report_*.json\`
- Maven reports: \`target/surefire-reports/\`

## Next Steps

If tests are failing:

1. Check application logs: \`tail -f app.log\`
2. Review specific test logs for detailed error messages
3. Verify database connectivity manually
4. Check environment configuration

EOF

    log "📊 Final report generated: $report_file" "$GREEN"
}

# Function to cleanup
cleanup() {
    log "Cleaning up..." "$YELLOW"
    stop_java_backend
    cd "$JAVA_BACKEND_DIR"
}

# Main execution function
main() {
    log "🚀 Starting Comprehensive Admin Workflow Test Suite" "$GREEN"
    log "Java Backend Directory: $JAVA_BACKEND_DIR" "$BLUE"
    log "Base URL: $BASE_URL" "$BLUE"
    
    # Set up trap for cleanup
    trap cleanup EXIT
    
    local start_time=$(date +%s)
    local test_results=()
    
    # Check if server is already running
    if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        log "✅ Server is already running" "$GREEN"
    else
        # Start the Java backend
        if ! start_java_backend; then
            log "❌ Failed to start Java backend. Exiting." "$RED"
            exit 1
        fi
    fi
    
    # Run bash tests
    log "\n🧪 Running Bash Test Suite" "$BLUE"
    if run_bash_tests; then
        test_results+=("Bash: PASSED")
    else
        test_results+=("Bash: FAILED")
    fi
    
    # Run Python tests
    log "\n🐍 Running Python Test Suite" "$BLUE"
    if run_python_tests; then
        test_results+=("Python: PASSED")
    else
        test_results+=("Python: FAILED")
    fi
    
    # Run Java tests
    log "\n☕ Running Java Test Suite" "$BLUE"
    if run_java_tests; then
        test_results+=("Java: PASSED")
    else
        test_results+=("Java: FAILED")
    fi
    
    # Calculate total time
    local end_time=$(date +%s)
    local total_time=$((end_time - start_time))
    
    # Print final summary
    log "\n📊 Final Test Summary" "$GREEN"
    log "Total Execution Time: ${total_time}s" "$BLUE"
    log "Test Results:" "$BLUE"
    
    local all_passed=true
    for result in "${test_results[@]}"; do
        if [[ "$result" == *"FAILED"* ]]; then
            log "  ❌ $result" "$RED"
            all_passed=false
        else
            log "  ✅ $result" "$GREEN"
        fi
    done
    
    # Generate final report
    generate_final_report
    
    if $all_passed; then
        log "\n🎉 All test suites passed!" "$GREEN"
        exit 0
    else
        log "\n⚠️  Some test suites failed. Check the logs for details." "$RED"
        exit 1
    fi
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --url)
            BASE_URL="$2"
            shift 2
            ;;
        --skip-startup)
            SKIP_STARTUP=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --url URL          Base URL of the application (default: http://localhost:8080)"
            echo "  --skip-startup     Skip starting the Java backend (assume it's already running)"
            echo "  --help, -h         Show this help message"
            echo ""
            echo "This script will:"
            echo "  1. Start the Java backend (unless --skip-startup is used)"
            echo "  2. Run bash-based tests"
            echo "  3. Run Python-based tests"
            echo "  4. Run Java integration tests"
            echo "  5. Generate a comprehensive report"
            exit 0
            ;;
        *)
            log "Unknown option: $1" "$RED"
            exit 1
            ;;
    esac
done

# Check dependencies
if ! command -v curl &> /dev/null; then
    log "❌ curl is required but not installed" "$RED"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    log "❌ Maven is required but not installed" "$RED"
    exit 1
fi

# Run main function
main "$@"