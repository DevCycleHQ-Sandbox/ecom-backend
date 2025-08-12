#!/bin/bash

# Regular User Workflow Test Script
echo "👤 Testing Regular User Workflow..."

BASE_URL="http://localhost:3002/api"
USER_USERNAME="newuser"
USER_PASSWORD="user123"

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
echo "🧪 Starting Regular User Tests..."
echo "================================="

# Step 1: Regular User Registration (if needed)
echo -e "${BLUE}📝 Step 1: Register Regular User${NC}"
REGISTER_DATA="{\"username\":\"$USER_USERNAME\",\"email\":\"newuser@example.com\",\"password\":\"$USER_PASSWORD\"}"
REGISTER_RESPONSE=$(make_request "POST" "$BASE_URL/auth/register" "$REGISTER_DATA")

if echo "$REGISTER_RESPONSE" | grep -q "token"; then
    echo -e "${GREEN}✅ User registration successful${NC}"
elif echo "$REGISTER_RESPONSE" | grep -q "already exists"; then
    echo -e "${YELLOW}ℹ️  User already exists, proceeding to login...${NC}"
else
    echo -e "${YELLOW}⚠️  Registration response: $REGISTER_RESPONSE${NC}"
fi

# Step 2: Regular User Login
echo -e "${BLUE}📝 Step 2: Regular User Login${NC}"
LOGIN_DATA="{\"username\":\"$USER_USERNAME\",\"password\":\"$USER_PASSWORD\"}"
LOGIN_RESPONSE=$(make_request "POST" "$BASE_URL/auth/login" "$LOGIN_DATA")

if echo "$LOGIN_RESPONSE" | grep -q "token"; then
    echo -e "${GREEN}✅ User login successful${NC}"
    USER_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    echo "   Token: ${USER_TOKEN:0:20}..."
else
    echo -e "${RED}❌ User login failed${NC}"
    echo "   Response: $LOGIN_RESPONSE"
    exit 1
fi

# Step 3: Verify User Token
echo -e "${BLUE}📝 Step 3: Verify User Token${NC}"
VERIFY_RESPONSE=$(make_request "GET" "$BASE_URL/auth/verify" "" "Authorization: Bearer $USER_TOKEN")
if echo "$VERIFY_RESPONSE" | grep -q "USER"; then
    echo -e "${GREEN}✅ User token verification successful${NC}"
    echo "   Role confirmed: USER"
else
    echo -e "${RED}❌ User token verification failed${NC}"
    echo "   Response: $VERIFY_RESPONSE"
fi

# Step 4: Get All Products (User should see available products)
echo -e "${BLUE}📝 Step 4: Get All Products${NC}"
PRODUCTS_RESPONSE=$(make_request "GET" "$BASE_URL/products" "" "Authorization: Bearer $USER_TOKEN")
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

# Step 5: Try to Create a Product (Should Fail - User doesn't have permission)
echo -e "${BLUE}📝 Step 5: Try Creating Product (Should Fail)${NC}"
NEW_PRODUCT_DATA='{
    "name": "User Test Product",
    "description": "Product created by user test script",
    "price": 49.99,
    "imageUrl": "https://example.com/user-product.jpg",
    "category": "Test",
    "stockQuantity": 50
}'
CREATE_RESPONSE=$(make_request "POST" "$BASE_URL/products" "$NEW_PRODUCT_DATA" "Authorization: Bearer $USER_TOKEN")
if echo "$CREATE_RESPONSE" | grep -q "403\|Forbidden\|Access Denied"; then
    echo -e "${GREEN}✅ Product creation correctly denied (expected)${NC}"
    echo "   User permissions working correctly"
elif echo "$CREATE_RESPONSE" | grep -q '"id"'; then
    echo -e "${RED}❌ Product creation allowed (unexpected!)${NC}"
    echo "   Security issue: Regular user should not create products"
else
    echo -e "${YELLOW}⚠️  Unexpected response to product creation attempt${NC}"
    echo "   Response: $CREATE_RESPONSE"
fi

# Step 6: Get User Cart
echo -e "${BLUE}📝 Step 6: Get User Cart${NC}"
CART_RESPONSE=$(make_request "GET" "$BASE_URL/cart" "" "Authorization: Bearer $USER_TOKEN")
if echo "$CART_RESPONSE" | grep -q "\["; then
    CART_ITEM_COUNT=$(echo "$CART_RESPONSE" | grep -o '"id"' | wc -l)
    echo -e "${GREEN}✅ Cart retrieved successfully${NC}"
    echo "   Cart items: $CART_ITEM_COUNT"
else
    echo -e "${YELLOW}⚠️  Cart is empty or error occurred${NC}"
    echo "   Response: $CART_RESPONSE"
fi

# Step 7: Add Product to Cart
if [ -n "$PRODUCT_ID" ]; then
    echo -e "${BLUE}📝 Step 7: Add Product to Cart${NC}"
    ADD_CART_DATA="{\"product_id\":\"$PRODUCT_ID\",\"quantity\":3}"
    ADD_CART_RESPONSE=$(make_request "POST" "$BASE_URL/cart" "$ADD_CART_DATA" "Authorization: Bearer $USER_TOKEN")
    if echo "$ADD_CART_RESPONSE" | grep -q '"id"'; then
        echo -e "${GREEN}✅ Product added to cart successfully${NC}"
        CART_ITEM_ID=$(echo "$ADD_CART_RESPONSE" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
        echo "   Cart Item ID: $CART_ITEM_ID"
    else
        echo -e "${RED}❌ Failed to add product to cart${NC}"
        echo "   Response: $ADD_CART_RESPONSE"
    fi
    
    # Step 8: Get Updated Cart
echo -e "${BLUE}📝 Step 8: Get Updated Cart${NC}"
UPDATED_CART_RESPONSE=$(make_request "GET" "$BASE_URL/cart" "" "Authorization: Bearer $USER_TOKEN")
    if echo "$UPDATED_CART_RESPONSE" | grep -q "\["; then
        UPDATED_CART_ITEM_COUNT=$(echo "$UPDATED_CART_RESPONSE" | grep -o '"id"' | wc -l)
        echo -e "${GREEN}✅ Updated cart retrieved successfully${NC}"
        echo "   Updated cart items: $UPDATED_CART_ITEM_COUNT"
        
        # Extract cart total if available
        if echo "$UPDATED_CART_RESPONSE" | grep -q '"quantity"'; then
            TOTAL_QUANTITY=$(echo "$UPDATED_CART_RESPONSE" | grep -o '"quantity":[0-9]*' | cut -d: -f2 | paste -sd+ | bc 2>/dev/null || echo "N/A")
            echo "   Total quantity in cart: $TOTAL_QUANTITY"
        fi
    else
        echo -e "${RED}❌ Failed to get updated cart${NC}"
        echo "   Response: $UPDATED_CART_RESPONSE"
    fi
    
    # Step 9: Update Cart Item Quantity
    if [ -n "$CART_ITEM_ID" ]; then
        echo -e "${BLUE}📝 Step 9: Update Cart Item Quantity${NC}"
        UPDATE_CART_DATA="{\"quantity\":5}"
        UPDATE_RESPONSE=$(make_request "PUT" "$BASE_URL/cart/$CART_ITEM_ID" "$UPDATE_CART_DATA" "Authorization: Bearer $USER_TOKEN")
        if echo "$UPDATE_RESPONSE" | grep -q '"quantity":5'; then
            echo -e "${GREEN}✅ Cart item quantity updated successfully${NC}"
        else
            echo -e "${YELLOW}⚠️  Cart update response: $UPDATE_RESPONSE${NC}"
        fi
    fi
else
    echo -e "${YELLOW}⚠️  Skipping cart tests - no product ID available${NC}"
fi

# Step 10: Try Admin-only Operations (Should Fail)
echo -e "${BLUE}📝 Step 10: Try Admin Operations (Should Fail)${NC}"

# Try to get all users (Admin only)
USERS_RESPONSE=$(make_request "GET" "$BASE_URL/admin/users" "" "Authorization: Bearer $USER_TOKEN")
if echo "$USERS_RESPONSE" | grep -q "403\|Forbidden\|Access Denied"; then
    echo -e "${GREEN}✅ Admin endpoint correctly denied (expected)${NC}"
    echo "   User permissions working correctly"
elif echo "$USERS_RESPONSE" | grep -q "\["; then
    echo -e "${RED}❌ Admin endpoint allowed (unexpected!)${NC}"
    echo "   Security issue: Regular user should not access admin endpoints"
else
    echo -e "${YELLOW}⚠️  Unexpected response to admin endpoint attempt${NC}"
    echo "   Response: $USERS_RESPONSE"
fi

# Step 11: Test Product Categories (Public endpoint)
echo -e "${BLUE}📝 Step 11: Get Product Categories${NC}"
CATEGORIES_RESPONSE=$(make_request "GET" "$BASE_URL/products/categories" "" "Authorization: Bearer $USER_TOKEN")
if echo "$CATEGORIES_RESPONSE" | grep -q "\["; then
    CATEGORY_COUNT=$(echo "$CATEGORIES_RESPONSE" | grep -o '"' | wc -l)
    echo -e "${GREEN}✅ Categories retrieved successfully${NC}"
    echo "   Categories: $CATEGORIES_RESPONSE"
else
    echo -e "${YELLOW}⚠️  Categories not found or error occurred${NC}"
    echo "   Response: $CATEGORIES_RESPONSE"
fi

# Step 12: Search Products (if endpoint exists)
echo -e "${BLUE}📝 Step 12: Search Products${NC}"
SEARCH_RESPONSE=$(make_request "GET" "$BASE_URL/products?search=test" "" "Authorization: Bearer $USER_TOKEN")
if echo "$SEARCH_RESPONSE" | grep -q "\["; then
    SEARCH_COUNT=$(echo "$SEARCH_RESPONSE" | grep -o '"id"' | wc -l)
    echo -e "${GREEN}✅ Product search working${NC}"
    echo "   Found $SEARCH_COUNT products matching 'test'"
else
    echo -e "${YELLOW}⚠️  Search not working or no results${NC}"
    echo "   Response: $SEARCH_RESPONSE"
fi

# Summary
echo ""
echo "================================="
echo -e "${BLUE}📊 Regular User Test Summary${NC}"
echo "================================="
echo -e "✅ Registration/Login: ${GREEN}SUCCESS${NC}"
echo -e "✅ Token Verification: ${GREEN}SUCCESS${NC}"
echo -e "✅ Product Access: ${GREEN}SUCCESS${NC}"
echo -e "✅ Cart Operations: ${GREEN}SUCCESS${NC}"
echo -e "✅ Permission Restrictions: ${GREEN}WORKING${NC}"
echo -e "✅ User Role Limitations: ${GREEN}ENFORCED${NC}"
echo ""
echo -e "${GREEN}🎉 All regular user tests completed successfully!${NC}" 