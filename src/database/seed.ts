import { DataSource } from "typeorm"
import * as bcrypt from "bcryptjs"
import { User } from "../entities/user.entity"
import { Product } from "../entities/product.entity"

export async function seedDatabase(dataSource: DataSource) {
  const userRepository = dataSource.getRepository(User)
  const productRepository = dataSource.getRepository(Product)

  // Check if data already exists
  const existingUsers = await userRepository.count()
  const existingProducts = await productRepository.count()

  if (existingUsers > 0 && existingProducts > 0) {
    console.log("🌱 Database already seeded")
    return
  }

  console.log("🌱 Seeding database...")

  // Create admin user
  const adminPassword = await bcrypt.hash("password", 12)
  const admin = userRepository.create({
    username: "admin",
    email: "admin@shopper.com",
    password: adminPassword,
    role: "admin",
  })
  await userRepository.save(admin)

  // Create regular user
  const userPassword = await bcrypt.hash("password", 12)
  const user = userRepository.create({
    username: "user",
    email: "user@shopper.com",
    password: userPassword,
    role: "user",
  })
  await userRepository.save(user)

  // Create sample products
  const products = [
    {
      name: "Wireless Bluetooth Headphones",
      description:
        "High-quality wireless headphones with noise cancellation and 30-hour battery life.",
      price: 99.99,
      image_url:
        "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500",
      category: "Electronics",
      stock_quantity: 50,
    },
    {
      name: "Ergonomic Office Chair",
      description:
        "Comfortable ergonomic office chair with lumbar support and adjustable height.",
      price: 299.99,
      image_url:
        "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=500",
      category: "Furniture",
      stock_quantity: 25,
    },
    {
      name: "Stainless Steel Water Bottle",
      description:
        "Eco-friendly stainless steel water bottle that keeps drinks cold for 24 hours.",
      price: 24.99,
      image_url:
        "https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=500",
      category: "Lifestyle",
      stock_quantity: 100,
    },
    {
      name: "Mechanical Gaming Keyboard",
      description:
        "RGB backlit mechanical keyboard with Cherry MX switches for gaming enthusiasts.",
      price: 149.99,
      image_url:
        "https://images.unsplash.com/photo-1541140532154-b024d705b90a?w=500",
      category: "Electronics",
      stock_quantity: 30,
    },
    {
      name: "Yoga Mat Premium",
      description:
        "Non-slip premium yoga mat with excellent grip and cushioning for all yoga styles.",
      price: 49.99,
      image_url:
        "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=500",
      category: "Fitness",
      stock_quantity: 75,
    },
    {
      name: "Smart Phone Stand",
      description:
        "Adjustable aluminum phone stand compatible with all smartphones and tablets.",
      price: 19.99,
      image_url:
        "https://images.unsplash.com/photo-1512499617640-c74ae3a79d37?w=500",
      category: "Accessories",
      stock_quantity: 150,
    },
    {
      name: "Coffee Bean Grinder",
      description:
        "Electric burr coffee grinder for the perfect cup of coffee every morning.",
      price: 79.99,
      image_url:
        "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=500",
      category: "Kitchen",
      stock_quantity: 40,
    },
    {
      name: "LED Desk Lamp",
      description:
        "Modern LED desk lamp with adjustable brightness and color temperature.",
      price: 34.99,
      image_url:
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500",
      category: "Home",
      stock_quantity: 60,
    },
    {
      name: "Wireless Mouse",
      description:
        "Ergonomic wireless mouse with precision tracking and long battery life.",
      price: 29.99,
      image_url:
        "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=500",
      category: "Electronics",
      stock_quantity: 80,
    },
    {
      name: "Backpack Laptop",
      description:
        "Durable laptop backpack with multiple compartments and water-resistant material.",
      price: 59.99,
      image_url:
        "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=500",
      category: "Accessories",
      stock_quantity: 45,
    },
  ]

  for (const productData of products) {
    const product = productRepository.create(productData)
    await productRepository.save(product)
  }

  console.log("✅ Database seeded successfully!")
  console.log("👤 Admin user: admin / password")
  console.log("👤 Regular user: user / password")
  console.log(`📦 Created ${products.length} sample products`)
}
