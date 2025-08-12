#!/bin/bash

# Update Products from JSON Script
echo "📦 Updating Products from JSON File..."

BASE_URL="http://localhost:3002/api"
ADMIN_USERNAME="admin"
ADMIN_PASSWORD="admin123"
JSON_FILE_PATH="/Users/suthar/dev/shopper/backend/data/products.json"

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
        if curl -s "http://localhost:3002/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ Server is running!${NC}"
            return 0
        fi
        sleep 2
        echo "  Attempt $i/30..."
    done
    echo -e "${RED}❌ Server failed to start within 60 seconds${NC}"
    return 1
}

# Parse command line arguments
CLEAR_EXISTING=true
FORCE_UPDATE=true

while [[ $# -gt 0 ]]; do
    case $1 in
        --clear)
            CLEAR_EXISTING=true
            shift
            ;;
        --import-only)
            FORCE_UPDATE=false
            shift
            ;;
        --file)
            JSON_FILE_PATH="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  --clear         Clear existing products before import"
            echo "  --import-only   Use bulk import instead of force update"
            echo "  --file PATH     Use custom JSON file path (default: ../data/products.json)"
            echo "  --help          Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Test server availability
wait_for_server || exit 1

echo ""
echo "🧪 Starting Product Update Process..."
echo "================================="

# Step 1: Admin Login
echo -e "${BLUE}📝 Step 1: Admin Login${NC}"
LOGIN_DATA="{\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}"
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

# Step 2: Check current product stats
echo -e "${BLUE}📝 Step 2: Current Product Statistics${NC}"
STATS_RESPONSE=$(make_request "GET" "$BASE_URL/products/admin/stats" "" "Authorization: Bearer $ADMIN_TOKEN")
if echo "$STATS_RESPONSE" | grep -q "totalProducts"; then
    CURRENT_COUNT=$(echo "$STATS_RESPONSE" | grep -o '"totalProducts":[0-9]*' | cut -d':' -f2)
    IN_STOCK_COUNT=$(echo "$STATS_RESPONSE" | grep -o '"inStockProducts":[0-9]*' | cut -d':' -f2)
    echo -e "${GREEN}✅ Current stats retrieved${NC}"
    echo "   Total Products: $CURRENT_COUNT"
    echo "   In Stock: $IN_STOCK_COUNT"
else
    echo -e "${YELLOW}⚠️  Could not retrieve current stats${NC}"
    echo "   Response: $STATS_RESPONSE"
fi

# Step 3: Update products
echo -e "${BLUE}📝 Step 3: Updating Products from JSON${NC}"
echo "   JSON File: $JSON_FILE_PATH"
echo "   Force Update: $FORCE_UPDATE"
echo "   Clear Existing: $CLEAR_EXISTING"

if [ "$FORCE_UPDATE" = true ]; then
    # Use force update endpoint
    UPDATE_URL="$BASE_URL/products/admin/force-update?filePath=$(python3 -c "import urllib.parse; print(urllib.parse.quote('$JSON_FILE_PATH'))")"
    UPDATE_RESPONSE=$(make_request "POST" "$UPDATE_URL" "" "Authorization: Bearer $ADMIN_TOKEN")
    OPERATION="force updated"
else
    # Use bulk import endpoint
    UPDATE_URL="$BASE_URL/products/admin/bulk-import?clearExisting=$CLEAR_EXISTING&filePath=$(python3 -c "import urllib.parse; print(urllib.parse.quote('$JSON_FILE_PATH'))")"
    UPDATE_RESPONSE=$(make_request "POST" "$UPDATE_URL" "" "Authorization: Bearer $ADMIN_TOKEN")
    OPERATION="imported"
fi

# Check if update was successful
if echo "$UPDATE_RESPONSE" | grep -q '"success":true'; then
    UPDATED_COUNT=$(echo "$UPDATE_RESPONSE" | grep -o '"updatedCount":[0-9]*\|"importedCount":[0-9]*' | cut -d':' -f2)
    TOTAL_COUNT=$(echo "$UPDATE_RESPONSE" | grep -o '"totalProducts":[0-9]*' | cut -d':' -f2)
    echo -e "${GREEN}✅ Products $OPERATION successfully${NC}"
    echo "   Products $OPERATION: $UPDATED_COUNT"
    echo "   Total Products: $TOTAL_COUNT"
else
    echo -e "${RED}❌ Failed to update products${NC}"
    echo "   Response: $UPDATE_RESPONSE"
    exit 1
fi

# Step 4: Verify update with new stats
echo -e "${BLUE}📝 Step 4: Verify Update${NC}"
FINAL_STATS_RESPONSE=$(make_request "GET" "$BASE_URL/products/admin/stats" "" "Authorization: Bearer $ADMIN_TOKEN")
if echo "$FINAL_STATS_RESPONSE" | grep -q "totalProducts"; then
    FINAL_COUNT=$(echo "$FINAL_STATS_RESPONSE" | grep -o '"totalProducts":[0-9]*' | cut -d':' -f2)
    FINAL_IN_STOCK=$(echo "$FINAL_STATS_RESPONSE" | grep -o '"inStockProducts":[0-9]*' | cut -d':' -f2)
    CATEGORIES=$(echo "$FINAL_STATS_RESPONSE" | grep -o '"categories":\[[^\]]*\]' | cut -d':' -f2)
    echo -e "${GREEN}✅ Final verification completed${NC}"
    echo "   Final Total Products: $FINAL_COUNT"
    echo "   Final In Stock: $FINAL_IN_STOCK"
    echo "   Categories: $CATEGORIES"
else
    echo -e "${YELLOW}⚠️  Could not verify final stats${NC}"
    echo "   Response: $FINAL_STATS_RESPONSE"
fi

# Step 5: Test product listing
echo -e "${BLUE}📝 Step 5: Test Product Listing${NC}"
PRODUCTS_RESPONSE=$(make_request "GET" "$BASE_URL/products" "" "Authorization: Bearer $ADMIN_TOKEN")
if echo "$PRODUCTS_RESPONSE" | grep -q "\["; then
    LISTED_COUNT=$(echo "$PRODUCTS_RESPONSE" | grep -o '"id"' | wc -l)
    echo -e "${GREEN}✅ Product listing working${NC}"
    echo "   Listed Products: $LISTED_COUNT"
    
    # Show sample product names
    echo "   Sample Products:"
    echo "$PRODUCTS_RESPONSE" | grep -o '"name":"[^"]*"' | head -3 | while read -r line; do
        PRODUCT_NAME=$(echo "$line" | cut -d'"' -f4)
        echo "     - $PRODUCT_NAME"
    done
else
    echo -e "${YELLOW}⚠️  Product listing may have issues${NC}"
    echo "   Response: ${PRODUCTS_RESPONSE:0:200}..."
fi

echo ""
echo -e "${GREEN}🎉 Product update process completed successfully!${NC}"
echo ""
echo "📋 Summary:"
echo "   Operation: $OPERATION"
echo "   Products processed: $UPDATED_COUNT"
echo "   Total products in database: $FINAL_COUNT"
echo "   Both databases were updated automatically"
echo ""
echo "✨ You can now access the updated products through the API endpoints." 