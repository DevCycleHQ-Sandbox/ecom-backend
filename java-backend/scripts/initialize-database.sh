#!/bin/bash

# Database Initialization Script
# Creates admin user and sample products for testing

set -e  # Exit on any error

BASE_URL="http://localhost:8080"
ADMIN_USERNAME="admin@shopper.com"
ADMIN_PASSWORD="admin123"
ADMIN_EMAIL="admin@shopper.com"

echo "🚀 Initializing Database with Sample Data..."
echo "============================================="

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

# Wait for application to be ready
echo "⏳ Waiting for application to be ready..."
max_attempts=30
attempt=1

while [ $attempt -le $max_attempts ]; do
    health_response=$(curl -s "$BASE_URL/actuator/health" --write-out "\nHTTP_CODE:%{http_code}\n" --max-time 5 || echo "HTTP_CODE:000")
    health_code=$(extract_http_code "$health_response")
    
    if [ "$health_code" = "200" ]; then
        echo "✅ Application is ready!"
        break
    else
        echo "⏳ Attempt $attempt/$max_attempts - Application not ready yet (code: $health_code)"
        sleep 2
        attempt=$((attempt + 1))
    fi
done

if [ $attempt -gt $max_attempts ]; then
    echo "❌ Application failed to start within expected time"
    exit 1
fi

# Step 1: Create Admin User
echo ""
echo "👤 Step 1: Creating Admin User"
admin_register_data='{
    "username": "'$ADMIN_USERNAME'",
    "email": "'$ADMIN_EMAIL'",
    "password": "'$ADMIN_PASSWORD'"
}'

register_response=$(make_request "POST" "$BASE_URL/auth/register" "$admin_register_data")
register_code=$(extract_http_code "$register_response")

if [ "$register_code" = "200" ]; then
    echo "✅ Admin user created successfully"
    jwt_token=$(extract_json "$register_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
elif [ "$register_code" = "400" ]; then
    echo "ℹ️  Admin user already exists, logging in..."
    
    # Login instead
    admin_login_data='{
        "username": "'$ADMIN_USERNAME'",
        "password": "'$ADMIN_PASSWORD'"
    }'
    
    login_response=$(make_request "POST" "$BASE_URL/auth/login" "$admin_login_data")
    login_code=$(extract_http_code "$login_response")
    
    if [ "$login_code" = "200" ]; then
        echo "✅ Admin login successful"
        jwt_token=$(extract_json "$login_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    else
        echo "❌ Admin login failed"
        exit 1
    fi
else
    echo "❌ Admin registration failed with code: $register_code"
    echo "Response: $(extract_json "$register_response")"
    exit 1
fi

# Step 2: Create Sample Products
echo ""
echo "📦 Step 2: Creating Sample Products"

# Array of sample products
declare -a products=(
    '{"name": "Laptop Pro", "description": "High-performance laptop for professionals", "price": 1299.99, "imageUrl": "https://example.com/laptop.jpg", "category": "Electronics", "stockQuantity": 50}'
    '{"name": "Wireless Mouse", "description": "Ergonomic wireless mouse", "price": 29.99, "imageUrl": "https://example.com/mouse.jpg", "category": "Electronics", "stockQuantity": 100}'
    '{"name": "Coffee Mug", "description": "Premium ceramic coffee mug", "price": 12.99, "imageUrl": "https://example.com/mug.jpg", "category": "Home & Kitchen", "stockQuantity": 200}'
    '{"name": "Running Shoes", "description": "Comfortable running shoes", "price": 89.99, "imageUrl": "https://example.com/shoes.jpg", "category": "Sports", "stockQuantity": 75}'
    '{"name": "Smartphone", "description": "Latest smartphone with advanced features", "price": 799.99, "imageUrl": "https://example.com/phone.jpg", "category": "Electronics", "stockQuantity": 30}'
    '{"name": "Book - Tech Guide", "description": "Comprehensive guide to modern technology", "price": 24.99, "imageUrl": "https://example.com/book.jpg", "category": "Books", "stockQuantity": 150}'
    '{"name": "Desk Lamp", "description": "LED desk lamp with adjustable brightness", "price": 39.99, "imageUrl": "https://example.com/lamp.jpg", "category": "Home & Kitchen", "stockQuantity": 80}'
    '{"name": "Wireless Headphones", "description": "Noise-cancelling wireless headphones", "price": 199.99, "imageUrl": "https://example.com/headphones.jpg", "category": "Electronics", "stockQuantity": 60}'
)

created_count=0
failed_count=0

for product_data in "${products[@]}"; do
    create_response=$(make_request "POST" "$BASE_URL/api/products" "$product_data" "Authorization: Bearer $jwt_token")
    create_code=$(extract_http_code "$create_response")
    
    if [ "$create_code" = "200" ]; then
        created_count=$((created_count + 1))
        product_info=$(extract_json "$create_response")
        product_name=$(echo "$product_info" | grep -o '"name":"[^"]*"' | cut -d'"' -f4)
        echo "✅ Created: $product_name"
    else
        failed_count=$((failed_count + 1))
        echo "❌ Failed to create product (code: $create_code)"
        echo "   Data: $product_data"
        echo "   Response: $(extract_json "$create_response")"
    fi
done

echo ""
echo "📊 Product Creation Summary:"
echo "   ✅ Created: $created_count products"
echo "   ❌ Failed: $failed_count products"

# Step 3: Create Test User
echo ""
echo "👥 Step 3: Creating Test User"
user_register_data='{
    "username": "testuser",
    "email": "test@example.com",
    "password": "testpass123"
}'

user_register_response=$(make_request "POST" "$BASE_URL/auth/register" "$user_register_data")
user_register_code=$(extract_http_code "$user_register_response")

if [ "$user_register_code" = "200" ]; then
    echo "✅ Test user created successfully"
    user_jwt_token=$(extract_json "$user_register_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
elif [ "$user_register_code" = "400" ]; then
    echo "ℹ️  Test user already exists"
    
    # Login the test user
    user_login_data='{
        "username": "testuser",
        "password": "testpass123"
    }'
    
    user_login_response=$(make_request "POST" "$BASE_URL/auth/login" "$user_login_data")
    user_login_code=$(extract_http_code "$user_login_response")
    
    if [ "$user_login_code" = "200" ]; then
        echo "✅ Test user login successful"
        user_jwt_token=$(extract_json "$user_login_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    fi
else
    echo "⚠️  Test user creation failed, but continuing..."
fi

# Step 4: Add items to test user's cart (if test user exists)
if [ -n "$user_jwt_token" ]; then
    echo ""
    echo "🛒 Step 4: Adding Items to Test User's Cart"
    
    # Get products to add to cart
    products_response=$(make_request "GET" "$BASE_URL/api/products" "" "Authorization: Bearer $user_jwt_token")
    products_code=$(extract_http_code "$products_response")
    
    if [ "$products_code" = "200" ]; then
        products_list=$(extract_json "$products_response")
        # Extract first few product IDs for cart items
        first_product_id=$(echo "$products_list" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
        second_product_id=$(echo "$products_list" | grep -o '"id":"[^"]*"' | head -2 | tail -1 | cut -d'"' -f4)
        
        if [ -n "$first_product_id" ]; then
            # Add first product to cart
            cart_item1='{
                "productId": "'$first_product_id'",
                "quantity": 2
            }'
            
            cart_response1=$(make_request "POST" "$BASE_URL/cart" "$cart_item1" "Authorization: Bearer $user_jwt_token")
            cart_code1=$(extract_http_code "$cart_response1")
            
            if [ "$cart_code1" = "200" ]; then
                echo "✅ Added first product to cart"
            else
                echo "⚠️  Failed to add first product to cart"
            fi
        fi
        
        if [ -n "$second_product_id" ]; then
            # Add second product to cart
            cart_item2='{
                "productId": "'$second_product_id'",
                "quantity": 1
            }'
            
            cart_response2=$(make_request "POST" "$BASE_URL/cart" "$cart_item2" "Authorization: Bearer $user_jwt_token")
            cart_code2=$(extract_http_code "$cart_response2")
            
            if [ "$cart_code2" = "200" ]; then
                echo "✅ Added second product to cart"
            else
                echo "⚠️  Failed to add second product to cart"
            fi
        fi
    fi
fi

# Step 5: Final verification
echo ""
echo "🔍 Step 5: Final Verification"

# Check products count
products_response=$(make_request "GET" "$BASE_URL/api/products" "" "Authorization: Bearer $jwt_token")
products_code=$(extract_http_code "$products_response")

if [ "$products_code" = "200" ]; then
    products_list=$(extract_json "$products_response")
    total_products=$(echo "$products_list" | grep -o '"id":' | wc -l)
    echo "✅ Total products in database: $total_products"
else
    echo "❌ Failed to verify products"
fi

# Check categories
categories_response=$(make_request "GET" "$BASE_URL/api/products/categories" "" "Authorization: Bearer $jwt_token")
categories_code=$(extract_http_code "$categories_response")

if [ "$categories_code" = "200" ]; then
    categories_list=$(extract_json "$categories_response")
    echo "✅ Available categories: $categories_list"
else
    echo "⚠️  Failed to get categories"
fi

echo ""
echo "🎉 Database Initialization Completed!"
echo "====================================="
echo ""
echo "📋 Summary:"
echo "   👤 Admin User: $ADMIN_USERNAME / $ADMIN_PASSWORD"
echo "   👥 Test User: testuser / testpass123"
echo "   📦 Products: $total_products total"
echo "   🛒 Cart: Test items added"
echo ""
echo "🚀 You can now:"
echo "   • Login as admin to manage products"
echo "   • Login as testuser to test shopping functionality"
echo "   • Test the admin workflow with: ./scripts/test-admin-workflow.sh"
echo "   • Test Neon connectivity with: ./scripts/test-neon-connectivity.sh" 