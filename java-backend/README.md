# Java Backend - Spring Boot E-commerce API

A comprehensive Java Spring Boot backend implementation that mirrors the functionality of the original NestJS backend.

## Features

- **Authentication & Authorization**: JWT-based authentication with role-based access control
- **Product Management**: CRUD operations for products with admin controls
- **Shopping Cart**: Add, update, remove items from cart
- **Order Management**: Create orders, track status, admin order management
- **User Management**: User registration, login, profile management
- **Admin Dashboard**: Database statistics, sync operations, system monitoring
- **Feature Flags**: Feature flag management for A/B testing and gradual rollouts
- **Security**: Spring Security with JWT tokens, password encryption
- **Database**: SQLite (primary) and PostgreSQL (secondary) support
- **API Documentation**: Swagger/OpenAPI documentation
- **Monitoring**: Spring Boot Actuator for health checks and metrics

## Tech Stack

- **Java 17+**
- **Spring Boot 3.2.0**
- **Spring Security 6**
- **Spring Data JPA**
- **SQLite & PostgreSQL**
- **JWT (JSON Web Tokens)**
- **Swagger/OpenAPI**
- **Maven**
- **Lombok**

## Project Structure

```
java-backend/
├── src/main/java/com/shopper/
│   ├── controller/          # REST controllers
│   ├── service/            # Business logic services
│   ├── repository/         # Data access layer
│   ├── entity/             # JPA entities
│   ├── dto/                # Data Transfer Objects
│   ├── security/           # Security configuration and JWT handling
│   ├── config/             # Application configuration
│   └── JavaBackendApplication.java
├── src/main/resources/
│   ├── application.yml     # Main configuration
│   ├── application-development.yml
│   ├── application-production.yml
│   └── data.sql           # Sample data
├── pom.xml                # Maven dependencies
└── README.md
```

## Quick Start

### Prerequisites

- Java 17 or later
- Maven 3.8+
- SQLite (for local database)
- PostgreSQL (optional, for secondary database)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd java-backend
   ```

2. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

3. **Build the project**
   ```bash
   mvn clean install
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

   Or run with environment variables:
   ```bash
   PORT=3002 JWT_SECRET=your-secret-key mvn spring-boot:run
   ```

5. **Access the application**
   - API Base URL: `http://localhost:3002/api`
   - Swagger UI: `http://localhost:3002/swagger-ui.html`
   - Health Check: `http://localhost:3002/actuator/health`

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user
- `GET /api/auth/verify` - Verify JWT token

### Products
- `GET /api/products` - Get all products
- `GET /api/products/{id}` - Get product by ID
- `POST /api/products` - Create product (Admin only)
- `PUT /api/products/{id}` - Update product (Admin only)
- `DELETE /api/products/{id}` - Delete product (Admin only)
- `GET /api/products/with-feature-flag` - Get products with feature flag evaluation
- `GET /api/products/premium-only` - Premium products (feature flag controlled)

### Cart
- `GET /api/cart` - Get cart items
- `POST /api/cart` - Add item to cart
- `PUT /api/cart/{id}` - Update cart item
- `DELETE /api/cart/{id}` - Remove cart item
- `DELETE /api/cart` - Clear cart

### Orders
- `POST /api/orders` - Create order
- `GET /api/orders` - Get user orders
- `GET /api/orders/{id}` - Get order by ID
- `PATCH /api/orders/{id}/status` - Update order status (Admin only)

### Admin
- `POST /api/admin/sync/all` - Sync all data
- `POST /api/admin/sync/{entity}` - Sync specific entity
- `GET /api/admin/database/stats` - Database statistics
- `GET /api/admin/sync/status` - Sync status

## Configuration

### Application Properties

Key configuration options in `application.yml`:

```yaml
server:
  port: 3002
  servlet:
    context-path: /api

app:
  jwt:
    secret: your-jwt-secret-key-here
    expiration: 86400000  # 24 hours

spring:
  datasource:
    primary:
      url: jdbc:sqlite:./database.sqlite
    secondary:
      url: jdbc:postgresql://localhost:5432/shopper
```

### Environment Variables

- `PORT` - Server port (default: 3002)
- `JWT_SECRET` - JWT secret key
- `DATABASE_URL` - Primary database URL
- `POSTGRES_URL` - Secondary database URL
- `FRONTEND_URL` - Frontend URL for CORS

## Security

- JWT-based authentication
- Role-based authorization (USER, ADMIN)
- Password encryption with BCrypt
- CORS configuration
- Request validation

## Development

### Running Tests
```bash
mvn test
```

### Building for Production
```bash
mvn clean package
java -jar target/java-backend-1.0.0.jar
```

### Database Migration
The application uses JPA with `ddl-auto: update` for development. 
For production, use `ddl-auto: validate` and proper database migrations.

## Feature Flags

The application includes a feature flag service that supports:
- Boolean flags
- String values
- Number values
- Object values
- Runtime flag updates

Example usage:
```java
boolean newFlow = featureFlagService.getBooleanValue(userId, "new-flow", false);
```

## Monitoring

Spring Boot Actuator provides:
- Health checks at `/actuator/health`
- Metrics at `/actuator/metrics`
- Application info at `/actuator/info`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.