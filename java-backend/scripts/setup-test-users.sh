#!/bin/bash

# Setup Test Users Script
echo "👥 Setting up test users..."

BASE_URL="http://localhost:3002/api"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to make HTTP requests
make_request() {
    local method=$1
    local url=$2
    local data=$3
    local headers=$4
    
    if [ -n "$data" ]; then
        if [ -n "$headers" ]; then
            curl -s -X "$method" "$url" -H "Content-Type: application/json" -H "$headers" -d "$data"
        else
            curl -s -X "$method" "$url" -H "Content-Type: application/json" -d "$data"
        fi
    else
        if [ -n "$headers" ]; then
            curl -s -X "$method" "$url" -H "$headers"
        else
            curl -s -X "$method" "$url"
        fi
    fi
}

# Wait for server
echo "⏳ Waiting for server to be ready..."
for i in {1..10}; do
    if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Server is ready!${NC}"
        break
    fi
    sleep 2
    echo "  Attempt $i/10..."
done

echo ""
echo "🔧 Creating test users..."
echo "========================"

# Create Admin User
echo -e "${BLUE}📝 Creating Admin User${NC}"
ADMIN_DATA='{
    "username": "admin",
    "email": "admin@example.com",
    "password": "admin123"
}'

ADMIN_RESPONSE=$(make_request "POST" "$BASE_URL/auth/register" "$ADMIN_DATA")
if echo "$ADMIN_RESPONSE" | grep -q "token"; then
    echo -e "${GREEN}✅ Admin user created successfully${NC}"
elif echo "$ADMIN_RESPONSE" | grep -q "already exists"; then
    echo -e "${YELLOW}ℹ️  Admin user already exists${NC}"
else
    echo -e "${YELLOW}⚠️  Admin creation response: $ADMIN_RESPONSE${NC}"
fi

# Create Regular User
echo -e "${BLUE}📝 Creating Regular User${NC}"
USER_DATA='{
    "username": "testuser",
    "email": "user@example.com",
    "password": "user123"
}'

USER_RESPONSE=$(make_request "POST" "$BASE_URL/auth/register" "$USER_DATA")
if echo "$USER_RESPONSE" | grep -q "token"; then
    echo -e "${GREEN}✅ Regular user created successfully${NC}"
elif echo "$USER_RESPONSE" | grep -q "already exists"; then
    echo -e "${YELLOW}ℹ️  Regular user already exists${NC}"
else
    echo -e "${YELLOW}⚠️  User creation response: $USER_RESPONSE${NC}"
fi

# Login as admin to get token and create some test products
echo -e "${BLUE}📝 Setting up test data as admin${NC}"
ADMIN_LOGIN_DATA='{
    "username": "admin",
    "password": "admin123"
}'

ADMIN_LOGIN_RESPONSE=$(make_request "POST" "$BASE_URL/auth/login" "$ADMIN_LOGIN_DATA")
if echo "$ADMIN_LOGIN_RESPONSE" | grep -q "token"; then
    ADMIN_TOKEN=$(echo "$ADMIN_LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    echo -e "${GREEN}✅ Admin login successful${NC}"
    
    # Create test products
    echo -e "${BLUE}📝 Creating test products${NC}"
    
    PRODUCT1_DATA='{
        "name": "Test Laptop",
        "description": "High-performance laptop for testing",
        "price": 999.99,
        "imageUrl": "https://example.com/laptop.jpg",
        "category": "Electronics",
        "stockQuantity": 50
    }'
    
    PRODUCT2_DATA='{
        "name": "Test Phone",
        "description": "Smartphone for testing",
        "price": 699.99,
        "imageUrl": "https://example.com/phone.jpg",
        "category": "Electronics",
        "stockQuantity": 100
    }'
    
    PRODUCT3_DATA='{
        "name": "Test Book",
        "description": "Educational book for testing",
        "price": 29.99,
        "imageUrl": "https://example.com/book.jpg",
        "category": "Books",
        "stockQuantity": 200
    }'
    
    for i in 1 2 3; do
        eval "PRODUCT_DATA=\$PRODUCT${i}_DATA"
        PRODUCT_RESPONSE=$(make_request "POST" "$BASE_URL/products" "$PRODUCT_DATA" "Authorization: Bearer $ADMIN_TOKEN")
        if echo "$PRODUCT_RESPONSE" | grep -q '"id"'; then
            PRODUCT_NAME=$(echo "$PRODUCT_DATA" | grep -o '"name":"[^"]*"' | cut -d'"' -f4)
            echo -e "${GREEN}  ✅ Created: $PRODUCT_NAME${NC}"
        else
            echo -e "${YELLOW}  ⚠️  Product $i response: $PRODUCT_RESPONSE${NC}"
        fi
    done
    
else
    echo -e "${RED}❌ Failed to login as admin${NC}"
fi

echo ""
echo "================================"
echo -e "${GREEN}🎉 Test setup completed!${NC}"
echo "================================"
echo ""
echo "Available test accounts:"
echo -e "  📧 Admin: ${BLUE}admin@example.com${NC} / ${BLUE}admin123${NC}"
echo -e "  📧 User:  ${BLUE}user@example.com${NC} / ${BLUE}user123${NC}"
echo ""
echo "Ready to run tests! 🚀" 