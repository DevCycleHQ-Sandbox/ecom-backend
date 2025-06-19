import { Injectable, Logger } from "@nestjs/common"
import { DatabaseService } from "./database.service"
import { DatabaseConfigService } from "./database-config.service"
import { DevCycleService } from "../devcycle/devcycle.service"

export interface DatabaseSwitchResult {
  success: boolean
  previousDatabase: "sqlite" | "postgres"
  currentDatabase: "sqlite" | "postgres"
  message: string
}

@Injectable()
export class DatabaseManagerService {
  private readonly logger = new Logger(DatabaseManagerService.name)

  constructor(
    private databaseService: DatabaseService,
    private databaseConfigService: DatabaseConfigService,
    private devCycleService: DevCycleService
  ) {}

  /**
   * Get current database information
   */
  async getCurrentDatabaseInfo(userId?: string) {
    const connectionInfo = this.databaseService.getConnectionInfo()
    const configuredType = userId
      ? await this.databaseConfigService.getDatabaseType(userId)
      : null

    return {
      ...connectionInfo,
      configuredType,
      featureFlagSupported: !!userId,
    }
  }

  /**
   * Check if user should use PostgreSQL based on feature flag
   */
  async shouldUsePostgreSQL(userId: string): Promise<boolean> {
    try {
      return await this.devCycleService.getVariableValue(
        userId,
        "use_postgres_database",
        false
      )
    } catch (error) {
      this.logger.warn(
        `Failed to get feature flag for user ${userId}:`,
        error.message
      )
      return false
    }
  }

  /**
   * Get database type for user based on feature flag
   */
  async getDatabaseTypeForUser(userId: string): Promise<"sqlite" | "postgres"> {
    const usePostgres = await this.shouldUsePostgreSQL(userId)
    return usePostgres ? "postgres" : "sqlite"
  }

  /**
   * Log database usage for analytics
   */
  async logDatabaseUsage(userId: string, operation: string) {
    const dbType = this.databaseService.getCurrentDatabaseType()
    const userDbType = await this.getDatabaseTypeForUser(userId)

    this.logger.log({
      event: "database_operation",
      userId,
      operation,
      actualDatabase: dbType,
      userConfiguredDatabase: userDbType,
      timestamp: new Date().toISOString(),
    })
  }

  /**
   * Validate database connection
   */
  async validateConnection(): Promise<{
    isValid: boolean
    database: string
    type: "sqlite" | "postgres"
    error?: string
  }> {
    try {
      const dataSource = this.databaseService.getDataSource()
      await dataSource.query("SELECT 1")

      const connectionInfo = this.databaseService.getConnectionInfo()

      return {
        isValid: true,
        database: connectionInfo.database,
        type: connectionInfo.type as "sqlite" | "postgres",
      }
    } catch (error) {
      return {
        isValid: false,
        database: "unknown",
        type: "sqlite",
        error: error.message,
      }
    }
  }

  /**
   * Get database performance metrics
   */
  async getDatabaseMetrics() {
    const connectionInfo = this.databaseService.getConnectionInfo()

    try {
      // Execute a simple query to check response time
      const startTime = Date.now()
      await this.databaseService.executeRawQuery("SELECT 1")
      const responseTime = Date.now() - startTime

      return {
        type: connectionInfo.type,
        database: connectionInfo.database,
        isConnected: connectionInfo.isConnected,
        responseTimeMs: responseTime,
        capabilities: {
          fullTextSearch: connectionInfo.type === "postgres",
          jsonSupport: connectionInfo.type === "postgres",
          arraySupport: connectionInfo.type === "postgres",
        },
      }
    } catch (error) {
      return {
        type: connectionInfo.type,
        database: connectionInfo.database,
        isConnected: false,
        error: error.message,
      }
    }
  }

  /**
   * Execute database-specific migrations or setup
   */
  async setupDatabaseSpecificFeatures(): Promise<void> {
    const dbType = this.databaseService.getCurrentDatabaseType()

    if (dbType === "postgres") {
      await this.setupPostgreSQLFeatures()
    } else {
      await this.setupSQLiteFeatures()
    }
  }

  private async setupPostgreSQLFeatures(): Promise<void> {
    try {
      // Enable UUID extension if needed
      await this.databaseService.executeRawQuery(
        'CREATE EXTENSION IF NOT EXISTS "uuid-ossp"'
      )
      this.logger.log("PostgreSQL UUID extension enabled")
    } catch (error) {
      this.logger.warn(
        "Failed to enable PostgreSQL UUID extension:",
        error.message
      )
    }
  }

  private async setupSQLiteFeatures(): Promise<void> {
    try {
      // Enable foreign keys for SQLite
      await this.databaseService.executeRawQuery("PRAGMA foreign_keys = ON")
      this.logger.log("SQLite foreign keys enabled")
    } catch (error) {
      this.logger.warn("Failed to enable SQLite foreign keys:", error.message)
    }
  }
}
