import { Injectable, OnModuleInit } from "@nestjs/common"
import { InjectDataSource } from "@nestjs/typeorm"
import { DataSource } from "typeorm"
import { DatabaseConfigService } from "./database-config.service"

@Injectable()
export class DatabaseService implements OnModuleInit {
  constructor(
    @InjectDataSource()
    private dataSource: DataSource,
    private databaseConfigService: DatabaseConfigService
  ) {}

  async onModuleInit() {
    // Initialize database if needed
    if (!this.dataSource.isInitialized) {
      await this.dataSource.initialize()
    }
  }

  /**
   * Get the current database type being used
   */
  getCurrentDatabaseType(): "sqlite" | "postgres" {
    return this.dataSource.options.type as "sqlite" | "postgres"
  }

  /**
   * Check if the application is using PostgreSQL
   */
  isUsingPostgreSQL(): boolean {
    return this.getCurrentDatabaseType() === "postgres"
  }

  /**
   * Check if the application is using SQLite
   */
  isUsingSQLite(): boolean {
    return this.getCurrentDatabaseType() === "sqlite"
  }

  /**
   * Get database-specific query builder methods
   */
  getDatabaseSpecificMethods() {
    const dbType = this.getCurrentDatabaseType()

    return {
      // Different date formatting for different databases
      formatDate: (date: Date): string => {
        if (dbType === "postgres") {
          return date.toISOString()
        }
        // SQLite
        return date.toISOString().slice(0, 19).replace("T", " ")
      },

      // Different LIMIT/OFFSET syntax handling
      getPaginationQuery: (limit: number, offset: number): string => {
        if (dbType === "postgres") {
          return `LIMIT ${limit} OFFSET ${offset}`
        }
        // SQLite
        return `LIMIT ${limit} OFFSET ${offset}`
      },

      // Different full-text search capabilities
      getSearchQuery: (column: string, searchTerm: string): string => {
        if (dbType === "postgres") {
          return `${column} ILIKE '%${searchTerm}%'`
        }
        // SQLite
        return `${column} LIKE '%${searchTerm}%'`
      },

      // UUID generation differences
      generateUUID: (): string => {
        if (dbType === "postgres") {
          // PostgreSQL can use gen_random_uuid() or uuid_generate_v4()
          return "uuid_generate_v4()"
        }
        // SQLite - TypeORM will handle UUID generation
        return "uuid()"
      },
    }
  }

  /**
   * Get the data source for direct queries
   */
  getDataSource(): DataSource {
    return this.dataSource
  }

  /**
   * Execute raw SQL with database-specific optimizations
   */
  async executeRawQuery(query: string, parameters?: any[]): Promise<any> {
    try {
      return await this.dataSource.query(query, parameters)
    } catch (error) {
      console.error(
        `Database query error (${this.getCurrentDatabaseType()}):`,
        error
      )
      throw error
    }
  }

  /**
   * Get database connection info for debugging
   */
  getConnectionInfo(): {
    type: string
    database: string
    isConnected: boolean
  } {
    const options = this.dataSource.options

    return {
      type: options.type,
      database:
        options.type === "postgres"
          ? (options as any).url ||
            `${(options as any).host}:${(options as any).port}/${(options as any).database}`
          : (options as any).database,
      isConnected: this.dataSource.isInitialized,
    }
  }
}
