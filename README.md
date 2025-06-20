# Shopper Backend API

A NestJS-based e-commerce backend with feature flags, dual database support, and OpenTelemetry monitoring.

## 🚀 Quick Start

```bash
npm install
npm run build
npm run dev
```

The API will be available at `http://localhost:3001/api`

## 🔧 Environment Setup

Copy `env.example` to `.env` and configure:

```bash
# Database
DATABASE_URL=./database.sqlite

# JWT
JWT_SECRET=your-jwt-secret

# Feature Flags
DEVCYCLE_SERVER_SDK_KEY=your-devcycle-server-key
DEVCYCLE_CLIENT_SDK_KEY=your-devcycle-client-key

# OpenTelemetry (Optional)
DYNATRACE_ENV_URL=your-dynatrace-url
DYNATRACE_API_TOKEN=your-dynatrace-token
USE_LOCAL_OTLP=false
LOCAL_OTLP_PORT=14499
```

## 👥 Default Users

The system comes with pre-configured test users:

| Username | Email             | Password   | Role  |
| -------- | ----------------- | ---------- | ----- |
| `admin`  | admin@shopper.com | `password` | admin |
| `user`   | user@shopper.com  | `password` | user  |

## 🏗️ Architecture

```
src/
├── main.ts                    # Application bootstrap
├── app.module.ts              # Root module
├── otelSetup.ts              # OpenTelemetry configuration
├── dynatraceOtelLogHook.ts   # Feature flag tracing
├── auth/                     # Authentication & authorization
├── users/                    # User management
├── products/                 # Product catalog
├── cart/                     # Shopping cart
├── orders/                   # Order processing
├── admin/                    # Admin operations
├── feature-flags/            # Feature flag service
├── database/                 # Database configuration & seeding
└── entities/                 # TypeORM entities
```

### Key Components

- **Authentication**: JWT-based auth with role guards
- **Database**: Dual SQLite/PostgreSQL support with TypeORM
- **Feature Flags**: DevCycle integration with OpenFeature
- **Monitoring**: OpenTelemetry with Dynatrace support
- **API**: RESTful endpoints with validation

## 🎛️ Feature Flags Setup

The app uses DevCycle with OpenFeature for feature management:

1. **Setup**: Configure `DEVCYCLE_SERVER_SDK_KEY` in environment
2. **Usage**: Inject `FeatureFlagService` into controllers/services
3. **Methods**:
   - `getBooleanValue(userId, key, defaultValue)`
   - `getStringValue(userId, key, defaultValue)`
   - `getNumberValue(userId, key, defaultValue)`
   - `getObjectValue(userId, key, defaultValue)`

### Current Feature Flags

| Flag Key   | Type    | Purpose                                                                                                 | Default | Usage                                            |
| ---------- | ------- | ------------------------------------------------------------------------------------------------------- | ------- | ------------------------------------------------ |
| `use-neon` | Boolean | Controls database routing - when `true`, reads from PostgreSQL (Neon), when `false` reads from SQLite   | `false` | Database abstraction layer for gradual migration |
| `new-flow` | Boolean | Enables intentional error generation for metric tracking - throws 500 error in 10% of product API calls | `false` | Error monitoring and alerting system validation  |

### Usage Examples

```typescript
// Database routing flag (currently implemented)
const useNeon = await this.featureFlagService.getBooleanValue(
  userId,
  "use-neon",
  false
)

// Error tracking flag (currently implemented)
const isNewFlowEnabled = await this.featureFlagService.getBooleanValue(
  userId,
  "new-flow",
  false
)

// Generic pattern for new flags
const isFeatureEnabled = await this.featureFlagService.getBooleanValue(
  userId,
  "feature-flag-key",
  false
)
```

### Database Feature Flag Details

The `use-neon` flag enables a dual-database architecture:

- **Write Operations**: Always write to both SQLite AND PostgreSQL
- **Read Operations**: Route based on feature flag per user
  - `false` (default): Read from SQLite (existing users)
  - `true`: Read from PostgreSQL/Neon (migrated users)
- **Benefits**: Zero-downtime database migration, A/B testing database performance

### Error Tracking Feature Flag Details

The `new-flow` flag enables intentional error generation for monitoring validation:

- **Purpose**: Validates that error tracking, monitoring, and alerting systems are working correctly
- **Behavior**: When enabled, throws HTTP 500 errors randomly in 10% of GET /api/products calls
- **Error Details**:
  - Error Type: `InternalServerErrorException`
  - Message: "Service temporarily unavailable - new flow processing error"
  - Logging: Includes user ID and random value for debugging
- **API Documentation**: Swagger/OpenAPI docs show potential 500 responses
- **Benefits**: Ensures observability tools capture and alert on real production errors

## 📊 OpenTelemetry & Monitoring

### Development Scripts

```bash
# Standard development
npm run dev

# With OpenTelemetry (Dynatrace)
npm run dev:otel

# With local OTLP collector
npm run dev:local-otlp
```

### Production Scripts

```bash
# Standard production
npm run start:prod

# With OpenTelemetry
npm run start:prod:otel
npm run start:prod:local-otlp
```

## 🚀 PM2 Deployment

### Available Configurations

1. **Standard Backend** (No OpenTelemetry)
2. **Backend with OpenTelemetry** (Dynatrace)
3. **Backend with Local OTLP**

### PM2 Commands

```bash
# Start standard backend
pm2 start ecosystem.config.js --only shopper-backend

# Start with OpenTelemetry (Dynatrace)
pm2 start ecosystem.config.js --only shopper-backend-otel

# Start with local OTLP collector
pm2 start ecosystem.config.js --only shopper-backend-local-otlp

# View logs
pm2 logs shopper-backend
pm2 logs shopper-backend-otel

# Monitor
pm2 monit

# Restart
pm2 restart shopper-backend

# Stop
pm2 stop shopper-backend

# View all processes
pm2 list
```

### PM2 Environment Configuration

Before deployment, update `ecosystem.config.js`:

- Set correct `cwd` path
- Configure environment variables
- Update database URLs
- Set proper API keys

## 📡 API Endpoints

### Authentication

- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login

### Products

- `GET /api/products` - List all products
- `GET /api/products/:id` - Get product details
- `POST /api/products` - Create product (admin)
- `PUT /api/products/:id` - Update product (admin)

### Cart

- `GET /api/cart` - Get user cart
- `POST /api/cart/add` - Add item to cart
- `PUT /api/cart/update/:id` - Update cart item
- `DELETE /api/cart/remove/:id` - Remove from cart

### Orders

- `GET /api/orders` - Get user orders
- `POST /api/orders` - Create order
- `GET /api/orders/:id` - Get order details

### Admin

- `GET /api/admin/users` - List all users (admin)
- `GET /api/admin/orders` - List all orders (admin)

## 🔍 Local Development

```bash
# Install dependencies
npm install

# Run in development mode
npm run dev

# Run with file watching
npm run start:debug

# Type checking
npm run type-check

# Linting
npm run lint
npm run lint:fix

# Formatting
npm run format
npm run format:check

# Run all checks
npm run check-all
```

## 🧪 Testing

```bash
# Unit tests
npm run test

# Test with coverage
npm run test:cov

# Watch mode
npm run test:watch

# E2E tests
npm run test:e2e
```

## 📝 Logs

PM2 logs are organized by service:

- Standard: `logs/out.log`, `logs/err.log`
- OpenTelemetry: `logs/otel-out.log`, `logs/otel-err.log`
- Local OTLP: `logs/local-otlp-out.log`, `logs/local-otlp-err.log`

## 🔧 Database

The app supports dual database configuration:

- **SQLite**: For development and testing
- **PostgreSQL**: For production

Database is automatically seeded with sample data on startup.

## 🛠️ Troubleshooting

### Common Issues

1. **Module not found**: Run `npm install`
2. **Database errors**: Check `DATABASE_URL` in `.env`
3. **Port conflicts**: Change `PORT` in environment
4. **Feature flag errors**: Verify `DEVCYCLE_SERVER_SDK_KEY`
5. **OpenTelemetry issues**: Check OTLP collector status

### Debug Mode

```bash
npm run start:debug
```

Access debugger at `chrome://inspect` in Chrome.
