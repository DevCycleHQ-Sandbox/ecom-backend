#!/bin/bash

# Comprehensive Test Runner
echo "🚀 Starting Comprehensive API Tests..."
echo "======================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:3002/api"

# Check if server is running
echo -e "${CYAN}🔍 Checking server status...${NC}"
if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Server is running on port 3002${NC}"
else
    echo -e "${RED}❌ Server is not running on port 3002${NC}"
    echo "Please start the server using: ./run-with-otel.sh"
    exit 1
fi

echo ""
echo -e "${CYAN}📊 Server Health Check:${NC}"
curl -s "$BASE_URL/actuator/health" | jq . || curl -s "$BASE_URL/actuator/health"

echo ""
echo "======================================"

# Step 1: Setup test users and data
echo -e "${CYAN}Step 1: Setting up test users and data${NC}"
echo "======================================"
if [ -f "scripts/setup-test-users.sh" ]; then
    chmod +x scripts/setup-test-users.sh
    ./scripts/setup-test-users.sh
    SETUP_EXIT_CODE=$?
    if [ $SETUP_EXIT_CODE -ne 0 ]; then
        echo -e "${RED}❌ Setup failed with exit code $SETUP_EXIT_CODE${NC}"
        exit 1
    fi
else
    echo -e "${RED}❌ Setup script not found: scripts/setup-test-users.sh${NC}"
    exit 1
fi

echo ""
echo "======================================"

# Step 2: Run Admin Tests
echo -e "${CYAN}Step 2: Running Admin User Tests${NC}"
echo "======================================"
if [ -f "scripts/test-admin-workflow.sh" ]; then
    chmod +x scripts/test-admin-workflow.sh
    ./scripts/test-admin-workflow.sh
    ADMIN_EXIT_CODE=$?
    echo ""
    if [ $ADMIN_EXIT_CODE -eq 0 ]; then
        echo -e "${GREEN}✅ Admin tests PASSED${NC}"
    else
        echo -e "${RED}❌ Admin tests FAILED with exit code $ADMIN_EXIT_CODE${NC}"
    fi
else
    echo -e "${RED}❌ Admin test script not found: scripts/test-admin-workflow.sh${NC}"
    ADMIN_EXIT_CODE=1
fi

echo ""
echo "======================================"

# Step 3: Run Regular User Tests
echo -e "${CYAN}Step 3: Running Regular User Tests${NC}"
echo "======================================"
if [ -f "scripts/test-user-workflow.sh" ]; then
    chmod +x scripts/test-user-workflow.sh
    ./scripts/test-user-workflow.sh
    USER_EXIT_CODE=$?
    echo ""
    if [ $USER_EXIT_CODE -eq 0 ]; then
        echo -e "${GREEN}✅ User tests PASSED${NC}"
    else
        echo -e "${RED}❌ User tests FAILED with exit code $USER_EXIT_CODE${NC}"
    fi
else
    echo -e "${RED}❌ User test script not found: scripts/test-user-workflow.sh${NC}"
    USER_EXIT_CODE=1
fi

echo ""
echo "======================================"

# Step 4: Additional API Tests
echo -e "${CYAN}Step 4: Running Additional API Tests${NC}"
echo "======================================"

# Test public endpoints
echo -e "${BLUE}🔍 Testing public endpoints${NC}"

# Health endpoint
HEALTH_RESPONSE=$(curl -s "$BASE_URL/actuator/health")
if echo "$HEALTH_RESPONSE" | grep -q "UP"; then
    echo -e "${GREEN}✅ Health endpoint working${NC}"
else
    echo -e "${RED}❌ Health endpoint failed${NC}"
fi

# OpenAPI documentation
SWAGGER_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/v3/api-docs")
if [ "$SWAGGER_RESPONSE" = "200" ]; then
    echo -e "${GREEN}✅ OpenAPI documentation available${NC}"
else
    echo -e "${YELLOW}⚠️  OpenAPI documentation not available (HTTP $SWAGGER_RESPONSE)${NC}"
fi

# Swagger UI
SWAGGER_UI_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/swagger-ui/index.html")
if [ "$SWAGGER_UI_RESPONSE" = "200" ]; then
    echo -e "${GREEN}✅ Swagger UI available${NC}"
else
    echo -e "${YELLOW}⚠️  Swagger UI not available (HTTP $SWAGGER_UI_RESPONSE)${NC}"
fi

echo ""
echo "======================================"

# Final Summary
echo -e "${CYAN}📊 Final Test Summary${NC}"
echo "======================================"

TOTAL_TESTS=0
PASSED_TESTS=0

if [ $ADMIN_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✅ Admin Workflow Tests: PASSED${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ Admin Workflow Tests: FAILED${NC}"
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

if [ $USER_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✅ User Workflow Tests: PASSED${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ User Workflow Tests: FAILED${NC}"
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo ""
echo -e "${BLUE}📈 Results: $PASSED_TESTS/$TOTAL_TESTS test suites passed${NC}"

if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    echo -e "${GREEN}🎉 ALL TESTS PASSED! 🎉${NC}"
    echo ""
    echo "✨ Your application is working correctly!"
    echo "📱 API endpoints are responsive"
    echo "🔐 Authentication system is functional"
    echo "🛒 Cart operations are working"
    echo "👤 User roles are properly enforced"
    echo ""
    echo "🌐 Access your application at: http://localhost:3002"
    echo "📚 API documentation: http://localhost:3002/swagger-ui/index.html"
    exit 0
else
    echo -e "${RED}❌ SOME TESTS FAILED${NC}"
    echo ""
    echo "🔍 Check the output above for details on failed tests"
    echo "🛠️  You may need to update the Java code to fix issues"
    exit 1
fi 