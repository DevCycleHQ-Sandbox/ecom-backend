#!/bin/bash

# PostgreSQL Schema Fix Script
# This script fixes the PostgreSQL database schema to use proper UUID columns

echo "🔧 Fixing PostgreSQL schema to use proper UUID columns..."

# Load environment variables
# Environment variables should already be set from the running application

# Check if we have the required environment variables
if [ -z "$NEON_DATABASE_URL" ]; then
    echo "❌ Error: NEON_DATABASE_URL environment variable is not set"
    exit 1
fi

# Parse the Neon URL to get connection details
if [[ $NEON_DATABASE_URL =~ ^postgresql://([^:]+):([^@]+)@([^/]+)/(.+) ]]; then
    DB_USER="${BASH_REMATCH[1]}"
    DB_PASSWORD="${BASH_REMATCH[2]}"
    DB_HOST_PORT="${BASH_REMATCH[3]}"
    DB_NAME="${BASH_REMATCH[4]}"
    
    # Split host and port
    if [[ $DB_HOST_PORT =~ ^([^:]+):?([0-9]*)$ ]]; then
        DB_HOST="${BASH_REMATCH[1]}"
        DB_PORT="${BASH_REMATCH[2]:-5432}"
    fi
else
    echo "❌ Error: Invalid NEON_DATABASE_URL format"
    exit 1
fi

echo "📡 Connecting to PostgreSQL database..."
echo "   Host: $DB_HOST:$DB_PORT"
echo "   Database: $DB_NAME"
echo "   User: $DB_USER"

# Try using docker with PostgreSQL client
if command -v docker &> /dev/null; then
    echo "🐳 Using Docker PostgreSQL client..."
    
    # Run the SQL fix script using Docker
    docker run --rm -i postgres:15 psql "$NEON_DATABASE_URL" << 'EOF'
-- Fix PostgreSQL schema to use proper UUID columns instead of VARCHAR
-- This script converts the existing VARCHAR(36) columns to UUID type

-- First, drop foreign key constraints if they exist
DO $$ 
BEGIN
    -- Drop foreign key constraint on cart_items.product_id if it exists
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints 
               WHERE constraint_name = 'fk_cart_items_product_id' 
               AND table_name = 'cart_items') THEN
        ALTER TABLE cart_items DROP CONSTRAINT fk_cart_items_product_id;
    END IF;
END $$;

-- Convert products table ID from VARCHAR to UUID
ALTER TABLE products ALTER COLUMN id TYPE UUID USING id::UUID;

-- Convert cart_items table columns from VARCHAR to UUID
ALTER TABLE cart_items ALTER COLUMN id TYPE UUID USING id::UUID;
ALTER TABLE cart_items ALTER COLUMN user_id TYPE UUID USING user_id::UUID;
ALTER TABLE cart_items ALTER COLUMN product_id TYPE UUID USING product_id::UUID;

-- Convert other tables if they exist
DO $$ 
BEGIN
    -- Convert orders table if it exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'orders') THEN
        ALTER TABLE orders ALTER COLUMN id TYPE UUID USING id::UUID;
        ALTER TABLE orders ALTER COLUMN user_id TYPE UUID USING user_id::UUID;
    END IF;
    
    -- Convert order_items table if it exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'order_items') THEN
        ALTER TABLE order_items ALTER COLUMN id TYPE UUID USING id::UUID;
        ALTER TABLE order_items ALTER COLUMN order_id TYPE UUID USING order_id::UUID;
        ALTER TABLE order_items ALTER COLUMN product_id TYPE UUID USING product_id::UUID;
    END IF;
    
    -- Convert users table if it exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        ALTER TABLE users ALTER COLUMN id TYPE UUID USING id::UUID;
    END IF;
END $$;

-- Re-add foreign key constraints
ALTER TABLE cart_items 
ADD CONSTRAINT fk_cart_items_product_id 
FOREIGN KEY (product_id) REFERENCES products(id);

-- Display the updated schema
SELECT 'cart_items schema:' as info;
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'cart_items' 
ORDER BY ordinal_position;

SELECT 'products schema:' as info;
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'products' 
ORDER BY ordinal_position;
EOF

    if [ $? -eq 0 ]; then
        echo "✅ PostgreSQL schema successfully updated to use UUID columns!"
        echo ""
        echo "🔄 Now running sync test to verify the fix..."
        ./scripts/sync-with-neon.sh
    else
        echo "❌ Failed to update PostgreSQL schema"
        exit 1
    fi
else
    echo "❌ Error: Docker is required to run PostgreSQL client"
    echo "   Please install Docker or install PostgreSQL client tools"
    exit 1
fi 