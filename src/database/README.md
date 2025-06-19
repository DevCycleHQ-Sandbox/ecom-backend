# Database System with Feature Flags

This database system supports both SQLite and PostgreSQL databases, with dynamic switching controlled by DevCycle feature flags.

## Overview

The system includes:

- **DatabaseConfigService**: Manages database configuration based on feature flags
- **DatabaseService**: Provides database-specific operations and utilities
- **DatabaseManagerService**: High-level database management and analytics
- **DatabaseMigrationService**: Handles database-specific setup and migrations
- **DatabaseController**: API endpoints for database monitoring and management

## Feature Flag Configuration

### DevCycle Feature Flag

The system uses a DevCycle feature flag named `use_postgres_database` to control which database each user uses:

- `true`: User will use PostgreSQL database
- `false`: User will use SQLite database (default)

### Environment Variables

```env
# Database Configuration
DATABASE_URL=./database.sqlite
DATABASE_TYPE=sqlite
POSTGRES_URL=postgresql://username:password@localhost:5432/shopper

# DevCycle Feature Flags
DEVCYCLE_SERVER_SDK_KEY=your-devcycle-server-sdk-key
DEVCYCLE_CLIENT_SDK_KEY=your-devcycle-client-sdk-key
```

## Database Setup

### SQLite Setup (Default)

SQLite requires no additional setup. The database file will be created automatically at the path specified in `DATABASE_URL`.

### PostgreSQL Setup

1. Install and start PostgreSQL
2. Create a database:
   ```sql
   CREATE DATABASE shopper;
   ```
3. Update the `POSTGRES_URL` environment variable
4. Set `DATABASE_TYPE=postgres` for startup configuration

## API Endpoints

All database endpoints require authentication. Admin-only endpoints require the `admin` role.

### Public Endpoints (Authenticated Users)

- `GET /database/status` - Get current database information for the user
- `GET /database/health` - Check database connection health

### Admin Endpoints

- `GET /database/metrics` - Get database performance metrics
- `GET /database/connection-info` - Get current database connection details
- `GET /database/stats` - Get database statistics
- `GET /database/user/:userId/database-type` - Check which database type a user should use

## Usage Examples

### Using Database Services in Your Code

```typescript
import { DatabaseService } from "../database/database.service"
import { DatabaseManagerService } from "../database/database-manager.service"

@Injectable()
export class YourService {
  constructor(
    private databaseService: DatabaseService,
    private databaseManagerService: DatabaseManagerService
  ) {}

  async someMethod(userId: string) {
    // Log database usage for analytics
    await this.databaseManagerService.logDatabaseUsage(userId, "your_operation")

    // Get database type being used
    const dbType = this.databaseService.getCurrentDatabaseType()

    // Use database-specific methods
    const dbMethods = this.databaseService.getDatabaseSpecificMethods()

    if (dbType === "postgres") {
      // PostgreSQL-specific logic
      // Use advanced features like full-text search
    } else {
      // SQLite-specific logic
      // Use basic SQL operations
    }
  }
}
```

### Database-Specific Query Examples

```typescript
// PostgreSQL full-text search
if (dbType === "postgres") {
  return await this.repository
    .createQueryBuilder("entity")
    .where("entity.search_vector @@ plainto_tsquery(:search)", {
      search: searchTerm,
    })
    .getMany()
}

// SQLite basic search
return await this.repository
  .createQueryBuilder("entity")
  .where("entity.name LIKE :searchPattern", {
    searchPattern: `%${searchTerm}%`,
  })
  .getMany()
```

## Database Features

### SQLite Features

- ✅ Lightweight and file-based
- ✅ No server setup required
- ✅ Perfect for development and small deployments
- ✅ Foreign key constraints (enabled automatically)
- ✅ WAL mode for better concurrency
- ❌ Limited concurrent write access
- ❌ No advanced full-text search
- ❌ No JSON operations

### PostgreSQL Features

- ✅ Full ACID compliance
- ✅ Excellent concurrent access
- ✅ Advanced full-text search with tsvector
- ✅ JSON/JSONB support
- ✅ Array data types
- ✅ Custom functions and triggers
- ✅ Partitioning and advanced indexing
- ❌ Requires server setup and maintenance
- ❌ More resource intensive

## Database-Specific Optimizations

The system automatically applies database-specific optimizations:

### SQLite Optimizations

- Foreign keys enabled: `PRAGMA foreign_keys = ON`
- WAL mode enabled: `PRAGMA journal_mode = WAL`
- Indexes created for common queries

### PostgreSQL Optimizations

- UUID extension enabled: `CREATE EXTENSION "uuid-ossp"`
- Concurrent indexes created for better performance
- Full-text search setup with triggers
- GIN indexes for text search

## Monitoring and Analytics

### Database Usage Logging

The system logs database operations for analytics:

```typescript
await this.databaseManagerService.logDatabaseUsage(userId, "operation_name")
```

### Health Checks

```typescript
const health = await this.databaseManagerService.validateConnection()
console.log(health) // { isValid: true, database: "...", type: "postgres" }
```

### Performance Metrics

```typescript
const metrics = await this.databaseManagerService.getDatabaseMetrics()
console.log(metrics.responseTimeMs) // Database response time
```

## Migration Between Databases

When switching between databases, consider:

1. **Data Migration**: Data needs to be exported from one database and imported to another
2. **Schema Differences**: Some PostgreSQL-specific features won't work in SQLite
3. **Performance**: PostgreSQL generally performs better under high load
4. **Deployment**: SQLite is easier to deploy, PostgreSQL requires server management

## Best Practices

1. **Feature Flag Gradual Rollout**: Start with a small percentage of users on PostgreSQL
2. **Monitor Performance**: Use the metrics endpoints to compare database performance
3. **Error Handling**: Always implement fallbacks for database-specific features
4. **Testing**: Test your application with both database types
5. **Backup Strategy**: Implement appropriate backup strategies for both databases

## Troubleshooting

### Common Issues

1. **PostgreSQL Connection Failed**

   - Check `POSTGRES_URL` environment variable
   - Ensure PostgreSQL server is running
   - Verify database exists and user has permissions

2. **SQLite File Permissions**

   - Ensure the application has write permissions to the database directory
   - Check that the database file isn't locked by another process

3. **Feature Flag Not Working**
   - Verify `DEVCYCLE_SERVER_SDK_KEY` is set correctly
   - Check that the feature flag `use_postgres_database` exists in DevCycle
   - Ensure the user ID is being passed correctly

### Debug Endpoints

Use these endpoints to debug database issues:

- `GET /database/status` - Check current database configuration
- `GET /database/health` - Verify database connectivity
- `GET /database/metrics` - Check database performance
- `GET /database/user/:userId/database-type` - Verify feature flag value for a user

## Environment-Specific Configuration

### Development

```env
DATABASE_TYPE=sqlite
DATABASE_URL=./dev-database.sqlite
```

### Production

```env
DATABASE_TYPE=postgres
POSTGRES_URL=postgresql://user:password@localhost:5432/shopper_prod
```

The system will automatically apply the appropriate optimizations and setup for each environment.
