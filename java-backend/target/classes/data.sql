-- Insert sample users
INSERT INTO users (id, username, email, password, role, created_at, updated_at) VALUES
    ('550e8400-e29b-41d4-a716-446655440000', 'admin', 'admin@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('550e8400-e29b-41d4-a716-446655440001', 'user1', 'user1@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('550e8400-e29b-41d4-a716-446655440002', 'user2', 'user2@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- Insert sample products
INSERT INTO products (id, name, description, price, image_url, category, stock_quantity, created_at, updated_at) VALUES
    ('650e8400-e29b-41d4-a716-446655440000', 'Wireless Headphones', 'High-quality wireless headphones with noise cancellation', 99.99, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500', 'Electronics', 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('650e8400-e29b-41d4-a716-446655440001', 'Smartphone', 'Latest model smartphone with advanced features', 699.99, 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500', 'Electronics', 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('650e8400-e29b-41d4-a716-446655440002', 'Running Shoes', 'Comfortable running shoes for all terrains', 89.99, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500', 'Sports', 75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('650e8400-e29b-41d4-a716-446655440003', 'Coffee Maker', 'Automatic coffee maker with programmable settings', 149.99, 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=500', 'Home', 25, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('650e8400-e29b-41d4-a716-446655440004', 'Desk Lamp', 'Modern LED desk lamp with adjustable brightness', 45.99, 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500', 'Home', 40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;