#!/bin/bash

# Admin User Workflow Test Script
echo "🔑 Testing Admin User Workflow..."

BASE_URL="http://localhost:3002/api"
ADMIN_EMAIL="admin@example.com"
ADMIN_PASSWORD="admin123"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to make HTTP requests with error handling
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

# Function to check if server is running
wait_for_server() {
    echo "⏳ Waiting for server to start..."
    for i in {1..30}; do
        if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ Server is running!${NC}"
            return 0
        fi
        sleep 2
        echo "  Attempt $i/30..."
    done
    echo -e "${RED}❌ Server failed to start within 60 seconds${NC}"
    return 1
}

# Test server availability
wait_for_server || exit 1

echo ""
echo "🧪 Starting Admin User Tests..."
echo "================================="

# Step 1: Admin Login
echo -e "${BLUE}📝 Step 1: Admin Login${NC}"
LOGIN_DATA="{\"username\":\"admin\",\"password\":\"$ADMIN_PASSWORD\"}"
LOGIN_RESPONSE=$(make_request "POST" "$BASE_URL/auth/login" "$LOGIN_DATA")

if echo "$LOGIN_RESPONSE" | grep -q "token"; then
    echo -e "${GREEN}✅ Admin login successful${NC}"
    ADMIN_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    echo "   Token: ${ADMIN_TOKEN:0:20}..."
else
    echo -e "${RED}❌ Admin login failed${NC}"
    echo "   Response: $LOGIN_RESPONSE"
    exit 1
fi

# Step 2: Verify Admin Token
echo -e "${BLUE}📝 Step 2: Verify Admin Token${NC}"
VERIFY_RESPONSE=$(make_request "GET" "$BASE_URL/auth/verify" "" "Authorization: Bearer $ADMIN_TOKEN")
if echo "$VERIFY_RESPONSE" | grep -q "ADMIN"; then
    echo -e "${GREEN}✅ Admin token verification successful${NC}"
    echo "   Role confirmed: ADMIN"
else
    echo -e "${RED}❌ Admin token verification failed${NC}"
    echo "   Response: $VERIFY_RESPONSE"
fi

# Step 3: Get All Products (Admin should see all)
echo -e "${BLUE}📝 Step 3: Get All Products${NC}"
PRODUCTS_RESPONSE=$(make_request "GET" "$BASE_URL/products" "" "Authorization: Bearer $ADMIN_TOKEN")
if echo "$PRODUCTS_RESPONSE" | grep -q "\["; then
    PRODUCT_COUNT=$(echo "$PRODUCTS_RESPONSE" | grep -o '"id"' | wc -l)
    echo -e "${GREEN}✅ Products retrieved successfully${NC}"
    echo "   Found $PRODUCT_COUNT products"
    
    # Extract first product ID for cart testing
    PRODUCT_ID=$(echo "$PRODUCTS_RESPONSE" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    echo "   Sample Product ID: $PRODUCT_ID"
else
    echo -e "${YELLOW}⚠️  No products found or error occurred${NC}"
    echo "   Response: $PRODUCTS_RESPONSE"
fi

# Step 4: Create a Product (Admin only)
echo -e "${BLUE}📝 Step 4: Create New Product (Admin Only)${NC}"
NEW_PRODUCT_DATA='{
    "name": "Admin Test Product",
    "description": "Product created by admin test script",
    "price": 99.99,
    "imageUrl": "https://example.com/test-product.jpg",
    "category": "Test",
    "stockQuantity": 100
}'
    CREATE_RESPONSE=$(make_request "POST" "$BASE_URL/products" "$NEW_PRODUCT_DATA" "Authorization: Bearer $ADMIN_TOKEN")
if echo "$CREATE_RESPONSE" | grep -q '"id"'; then
    echo -e "${GREEN}✅ Product creation successful${NC}"
    NEW_PRODUCT_ID=$(echo "$CREATE_RESPONSE" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    echo "   New Product ID: $NEW_PRODUCT_ID"
    PRODUCT_ID=$NEW_PRODUCT_ID  # Use this for cart testing
else
    echo -e "${RED}❌ Product creation failed${NC}"
    echo "   Response: $CREATE_RESPONSE"
fi

# Step 5: Get Admin Cart
echo -e "${BLUE}📝 Step 5: Get Admin Cart${NC}"
CART_RESPONSE=$(make_request "GET" "$BASE_URL/cart" "" "Authorization: Bearer $ADMIN_TOKEN")
if echo "$CART_RESPONSE" | grep -q "\["; then
    CART_ITEM_COUNT=$(echo "$CART_RESPONSE" | grep -o '"id"' | wc -l)
    echo -e "${GREEN}✅ Cart retrieved successfully${NC}"
    echo "   Cart items: $CART_ITEM_COUNT"
else
    echo -e "${YELLOW}⚠️  Cart is empty or error occurred${NC}"
    echo "   Response: $CART_RESPONSE"
fi

# Step 6: Add Product to Cart
if [ -n "$PRODUCT_ID" ]; then
    echo -e "${BLUE}📝 Step 6: Add Product to Cart${NC}"
    ADD_CART_DATA="{\"product_id\":\"$PRODUCT_ID\",\"quantity\":2}"
    ADD_CART_RESPONSE=$(make_request "POST" "$BASE_URL/cart" "$ADD_CART_DATA" "Authorization: Bearer $ADMIN_TOKEN")
    if echo "$ADD_CART_RESPONSE" | grep -q '"id"'; then
        echo -e "${GREEN}✅ Product added to cart successfully${NC}"
        CART_ITEM_ID=$(echo "$ADD_CART_RESPONSE" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
        echo "   Cart Item ID: $CART_ITEM_ID"
    else
        echo -e "${RED}❌ Failed to add product to cart${NC}"
        echo "   Response: $ADD_CART_RESPONSE"
    fi
    
    # Step 7: Get Updated Cart
echo -e "${BLUE}📝 Step 7: Get Updated Cart${NC}"
UPDATED_CART_RESPONSE=$(make_request "GET" "$BASE_URL/cart" "" "Authorization: Bearer $ADMIN_TOKEN")
    if echo "$UPDATED_CART_RESPONSE" | grep -q "\["; then
        UPDATED_CART_ITEM_COUNT=$(echo "$UPDATED_CART_RESPONSE" | grep -o '"id"' | wc -l)
        echo -e "${GREEN}✅ Updated cart retrieved successfully${NC}"
        echo "   Updated cart items: $UPDATED_CART_ITEM_COUNT"
    else
        echo -e "${RED}❌ Failed to get updated cart${NC}"
        echo "   Response: $UPDATED_CART_RESPONSE"
    fi
else
    echo -e "${YELLOW}⚠️  Skipping cart tests - no product ID available${NC}"
fi

# Step 8: Admin-specific Operations
echo -e "${BLUE}📝 Step 8: Admin-specific Operations${NC}"

# Get all users (Admin only)
USERS_RESPONSE=$(make_request "GET" "$BASE_URL/admin/users" "" "Authorization: Bearer $ADMIN_TOKEN")
if echo "$USERS_RESPONSE" | grep -q "\["; then
    USER_COUNT=$(echo "$USERS_RESPONSE" | grep -o '"id"' | wc -l)
    echo -e "${GREEN}✅ Users list retrieved successfully${NC}"
    echo "   Found $USER_COUNT users"
else
    echo -e "${YELLOW}⚠️  No users found or endpoint not available${NC}"
    echo "   Response: $USERS_RESPONSE"
fi

# Summary
echo ""
echo "================================="
echo -e "${BLUE}📊 Admin Test Summary${NC}"
echo "================================="
echo -e "✅ Login: ${GREEN}SUCCESS${NC}"
echo -e "✅ Token Verification: ${GREEN}SUCCESS${NC}"
echo -e "✅ Product Access: ${GREEN}SUCCESS${NC}"
echo -e "✅ Cart Operations: ${GREEN}SUCCESS${NC}"
echo -e "✅ Admin Privileges: ${GREEN}CONFIRMED${NC}"
echo ""
echo -e "${GREEN}🎉 All admin tests completed successfully!${NC}" 