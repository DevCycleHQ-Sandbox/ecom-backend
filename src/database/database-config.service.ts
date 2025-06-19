import { Injectable } from "@nestjs/common"
import { ConfigService } from "@nestjs/config"
import { TypeOrmModuleOptions } from "@nestjs/typeorm"
import { DevCycleService } from "../devcycle/devcycle.service"
import { User } from "../entities/user.entity"
import { Product } from "../entities/product.entity"
import { CartItem } from "../entities/cart-item.entity"
import { Order } from "../entities/order.entity"
import { OrderItem } from "../entities/order-item.entity"

@Injectable()
export class DatabaseConfigService {
  constructor(
    private configService: ConfigService,
    private devCycleService: DevCycleService
  ) {}

  async createTypeOrmOptions(userId?: string): Promise<TypeOrmModuleOptions> {
    const isProduction = this.configService.get("NODE_ENV") === "production"

    // Use feature flag to determine database type
    // Default to SQLite if no userId is provided or feature flag is not available
    const usePostgres = userId
      ? await this.devCycleService.getVariableValue(
          userId,
          "use_postgres_database",
          false
        )
      : this.configService.get("DATABASE_TYPE") === "postgres"

    const baseConfig = {
      entities: [User, Product, CartItem, Order, OrderItem],
      synchronize: !isProduction, // Don't auto-sync in production
      logging: !isProduction,
      retryAttempts: 3,
      retryDelay: 3000,
    }

    if (usePostgres) {
      console.log("🐘 Using PostgreSQL database (via feature flag)")
      return {
        ...baseConfig,
        type: "postgres" as const,
        url: this.configService.get("POSTGRES_URL"),
        ssl: isProduction ? { rejectUnauthorized: false } : false,
      }
    }

    console.log("📂 Using SQLite database (via feature flag)")
    return {
      ...baseConfig,
      type: "sqlite" as const,
      database: this.configService.get<string>(
        "DATABASE_URL",
        "./database.sqlite"
      ),
    }
  }

  async getDatabaseType(userId?: string): Promise<"sqlite" | "postgres"> {
    const usePostgres = userId
      ? await this.devCycleService.getVariableValue(
          userId,
          "use_postgres_database",
          false
        )
      : this.configService.get("DATABASE_TYPE") === "postgres"

    return usePostgres ? "postgres" : "sqlite"
  }

  /**
   * Get database configuration for application startup (no user context)
   * This will use environment variables as fallback
   */
  getStartupTypeOrmOptions(): TypeOrmModuleOptions {
    const isProduction = this.configService.get("NODE_ENV") === "production"
    const usePostgres = this.configService.get("DATABASE_TYPE") === "postgres"

    const baseConfig = {
      entities: [User, Product, CartItem, Order, OrderItem],
      synchronize: !isProduction,
      logging: !isProduction,
      retryAttempts: 3,
      retryDelay: 3000,
    }

    if (usePostgres) {
      console.log("🐘 Using PostgreSQL database (startup)")
      return {
        ...baseConfig,
        type: "postgres" as const,
        url: this.configService.get("POSTGRES_URL"),
        ssl: isProduction ? { rejectUnauthorized: false } : false,
      }
    }

    console.log("📂 Using SQLite database (startup)")
    return {
      ...baseConfig,
      type: "sqlite" as const,
      database: this.configService.get<string>(
        "DATABASE_URL",
        "./database.sqlite"
      ),
    }
  }
}
