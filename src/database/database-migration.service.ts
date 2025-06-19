import { Injectable, Logger } from "@nestjs/common"
import { InjectDataSource } from "@nestjs/typeorm"
import { DataSource } from "typeorm"
import { DatabaseService } from "./database.service"

@Injectable()
export class DatabaseMigrationService {
  private readonly logger = new Logger(DatabaseMigrationService.name)

  constructor(
    @InjectDataSource()
    private dataSource: DataSource,
    private databaseService: DatabaseService
  ) {}

  /**
   * Run database-specific setup and optimizations
   */
  async setupDatabase(): Promise<void> {
    const dbType = this.databaseService.getCurrentDatabaseType()

    this.logger.log(`Setting up ${dbType} database...`)

    if (dbType === "postgres") {
      await this.setupPostgreSQL()
    } else {
      await this.setupSQLite()
    }

    this.logger.log(`${dbType} database setup completed`)
  }

  /**
   * Setup PostgreSQL specific features
   */
  private async setupPostgreSQL(): Promise<void> {
    try {
      // Create extensions
      await this.dataSource.query('CREATE EXTENSION IF NOT EXISTS "uuid-ossp"')
      this.logger.log("PostgreSQL UUID extension enabled")

      // Create indexes for better performance
      await this.createPostgreSQLIndexes()

      // Set up full-text search if needed
      await this.setupPostgreSQLFullTextSearch()
    } catch (error) {
      this.logger.error("Failed to setup PostgreSQL features:", error.message)
    }
  }

  /**
   * Setup SQLite specific features
   */
  private async setupSQLite(): Promise<void> {
    try {
      // Enable foreign keys
      await this.dataSource.query("PRAGMA foreign_keys = ON")
      this.logger.log("SQLite foreign keys enabled")

      // Enable WAL mode for better concurrency
      await this.dataSource.query("PRAGMA journal_mode = WAL")
      this.logger.log("SQLite WAL mode enabled")

      // Create indexes for better performance
      await this.createSQLiteIndexes()
    } catch (error) {
      this.logger.error("Failed to setup SQLite features:", error.message)
    }
  }

  /**
   * Create PostgreSQL specific indexes
   */
  private async createPostgreSQLIndexes(): Promise<void> {
    const indexes = [
      // User indexes
      "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email ON users(email)",
      "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_username ON users(username)",

      // Product indexes
      "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_name ON products(name)",
      "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_category ON products(category)",

      // Order indexes
      "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_user_id ON orders(user_id)",
      "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_status ON orders(status)",
      "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_created_at ON orders(created_at)",

      // Cart indexes
      "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_cart_items_user_id ON cart_items(user_id)",
    ]

    for (const indexQuery of indexes) {
      try {
        await this.dataSource.query(indexQuery)
      } catch (error) {
        // Index might already exist, which is fine
        if (!error.message.includes("already exists")) {
          this.logger.warn(`Failed to create index: ${error.message}`)
        }
      }
    }
  }

  /**
   * Create SQLite specific indexes
   */
  private async createSQLiteIndexes(): Promise<void> {
    const indexes = [
      // User indexes
      "CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)",
      "CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)",

      // Product indexes
      "CREATE INDEX IF NOT EXISTS idx_products_name ON products(name)",
      "CREATE INDEX IF NOT EXISTS idx_products_category ON products(category)",

      // Order indexes
      "CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id)",
      "CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status)",
      "CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at)",

      // Cart indexes
      "CREATE INDEX IF NOT EXISTS idx_cart_items_user_id ON cart_items(user_id)",
    ]

    for (const indexQuery of indexes) {
      try {
        await this.dataSource.query(indexQuery)
      } catch (error) {
        this.logger.warn(`Failed to create SQLite index: ${error.message}`)
      }
    }
  }

  /**
   * Setup PostgreSQL full-text search
   */
  private async setupPostgreSQLFullTextSearch(): Promise<void> {
    try {
      // Add text search columns if they don't exist
      const addTsVectorQuery = `
        ALTER TABLE products 
        ADD COLUMN IF NOT EXISTS search_vector tsvector
      `
      await this.dataSource.query(addTsVectorQuery)

      // Create trigger to update search vector
      const createTriggerQuery = `
        CREATE OR REPLACE FUNCTION update_product_search_vector()
        RETURNS trigger AS $$
        BEGIN
          NEW.search_vector = to_tsvector('english', 
            COALESCE(NEW.name, '') || ' ' || 
            COALESCE(NEW.description, '') || ' ' || 
            COALESCE(NEW.category, '')
          );
          RETURN NEW;
        END;
        $$ LANGUAGE plpgsql;
        
        DROP TRIGGER IF EXISTS update_product_search_vector_trigger ON products;
        CREATE TRIGGER update_product_search_vector_trigger
          BEFORE INSERT OR UPDATE ON products
          FOR EACH ROW EXECUTE FUNCTION update_product_search_vector();
      `
      await this.dataSource.query(createTriggerQuery)

      // Create GIN index for full-text search
      await this.dataSource.query(
        "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_search_vector ON products USING gin(search_vector)"
      )

      this.logger.log("PostgreSQL full-text search setup completed")
    } catch (error) {
      this.logger.warn(
        "Failed to setup PostgreSQL full-text search:",
        error.message
      )
    }
  }

  /**
   * Get database statistics
   */
  async getDatabaseStats(): Promise<any> {
    const dbType = this.databaseService.getCurrentDatabaseType()

    if (dbType === "postgres") {
      return await this.getPostgreSQLStats()
    } else {
      return await this.getSQLiteStats()
    }
  }

  private async getPostgreSQLStats(): Promise<any> {
    try {
      const result = await this.dataSource.query(`
        SELECT 
          schemaname,
          tablename,
          n_tup_ins as inserts,
          n_tup_upd as updates,
          n_tup_del as deletes,
          n_live_tup as live_tuples,
          n_dead_tup as dead_tuples
        FROM pg_stat_user_tables
        ORDER BY schemaname, tablename
      `)

      return {
        type: "postgres",
        tables: result,
      }
    } catch (error) {
      return {
        type: "postgres",
        error: error.message,
      }
    }
  }

  private async getSQLiteStats(): Promise<any> {
    try {
      const tables = await this.dataSource.query(`
        SELECT name FROM sqlite_master WHERE type='table'
      `)

      const stats = []
      for (const table of tables) {
        const count = await this.dataSource.query(
          `SELECT COUNT(*) as count FROM ${table.name}`
        )
        stats.push({
          table: table.name,
          count: count[0].count,
        })
      }

      return {
        type: "sqlite",
        tables: stats,
      }
    } catch (error) {
      return {
        type: "sqlite",
        error: error.message,
      }
    }
  }
}
