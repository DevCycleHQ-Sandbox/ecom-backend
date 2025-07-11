# Java Backend Service

A comprehensive Spring Boot REST API backend service for the shopping webapp, providing all the functionality from the original NestJS backend.

## Features

- **Authentication & Authorization**
  - JWT-based authentication
  - Role-based access control (USER/ADMIN)
  - Secure password hashing with BCrypt

- **User Management**
  - User registration and login
  - Profile management
  - Role-based permissions

- **Product Management**
  - CRUD operations for products
  - Category-based filtering
  - Stock management
  - Search functionality

- **Shopping Cart**
  - Add/remove items from cart
  - Update quantities
  - Persistent cart storage

- **Order Management**
  - Create orders from cart
  - Order history
  - Order status management
  - Admin order oversight

- **Feature Flags**
  - Configurable feature toggles
  - User-specific feature targeting
  - A/B testing support

- **Admin Panel**
  - Database synchronization
  - System statistics
  - Admin-only endpoints

- **Database Support**
  - SQLite (primary) and PostgreSQL (secondary)
  - Dual database configuration
  - Database migration support

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.5**
- **Spring Security** (JWT authentication)
- **Spring Data JPA** (data persistence)
- **SQLite** (primary database)
- **PostgreSQL** (secondary database)
- **Maven** (build tool)
- **Swagger/OpenAPI** (API documentation)

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- PostgreSQL (optional, for secondary database)

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd java-backend
```

2. Configure environment variables:
```bash
cp .env.example .env
# Edit .env with your configurations
```

3. Build the project:
```bash
mvn clean install
```

4. Run the application:
```bash
mvn spring-boot:run
```

The server will start on `http://localhost:3002/api`

### Environment Variables

Create a `.env` file in the project root:

```env
# Server Configuration
PORT=3002

# Database Configuration
POSTGRES_URL=jdbc:postgresql://localhost:5432/shopper_java
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-here-make-it-long-and-secure

# Feature Flags
DEVCYCLE_SERVER_SDK_KEY=your-devcycle-server-sdk-key
DEVCYCLE_CLIENT_SDK_KEY=your-devcycle-client-sdk-key

# CORS Configuration
FRONTEND_URL=http://localhost:3000
```

## API Documentation

Once the application is running, you can access the Swagger UI at:
`http://localhost:3002/api/swagger-ui.html`

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - User login
- `GET /api/auth/verify` - Verify JWT token

### Products
- `GET /api/products` - Get all products
- `POST /api/products` - Create product (admin only)
- `GET /api/products/{id}` - Get product by ID
- `PUT /api/products/{id}` - Update product (admin only)
- `DELETE /api/products/{id}` - Delete product (admin only)
- `GET /api/products/categories` - Get all categories
- `GET /api/products/category/{category}` - Get products by category

### Shopping Cart
- `GET /api/cart` - Get user's cart
- `POST /api/cart` - Add item to cart
- `PUT /api/cart/{id}` - Update cart item
- `DELETE /api/cart/{id}` - Remove item from cart
- `DELETE /api/cart` - Clear cart

### Orders
- `GET /api/orders` - Get user's orders
- `POST /api/orders` - Create new order
- `GET /api/orders/{id}` - Get order by ID
- `PATCH /api/orders/{id}/status` - Update order status (admin only)

### Admin
- `POST /api/admin/sync/all` - Sync all data
- `POST /api/admin/sync/{entity}` - Sync specific entity
- `GET /api/admin/database/stats` - Get database statistics
- `GET /api/admin/sync/status` - Get sync status

## Database Schema

The application uses JPA entities with the following main tables:

- `users` - User accounts and authentication
- `products` - Product catalog
- `cart_items` - Shopping cart items
- `orders` - Customer orders
- `order_items` - Individual items in orders

## Security

- JWT tokens for authentication
- Role-based access control
- Password hashing with BCrypt
- CORS configuration
- Security headers

## Feature Flags

The application includes a feature flag system for:
- Enhanced product views
- Premium features
- Cart improvements
- Order tracking
- A/B testing

## Testing

Run tests with:
```bash
mvn test
```

## Production Deployment

1. Build the JAR file:
```bash
mvn clean package
```

2. Run with production profile:
```bash
java -jar target/java-backend-1.0.0.jar --spring.profiles.active=production
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests
5. Submit a pull request

## License

This project is licensed under the MIT License.