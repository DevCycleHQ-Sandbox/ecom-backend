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
\d cart_items;
\d products; 