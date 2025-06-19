import { Module } from "@nestjs/common"
import { TypeOrmModule } from "@nestjs/typeorm"
import { ConfigModule, ConfigService } from "@nestjs/config"
import { User } from "../entities/user.entity"
import { Product } from "../entities/product.entity"
import { CartItem } from "../entities/cart-item.entity"
import { Order } from "../entities/order.entity"
import { OrderItem } from "../entities/order-item.entity"
import { DevCycleModule } from "../devcycle/devcycle.module"
import { DatabaseConfigService } from "./database-config.service"
import { DatabaseService } from "./database.service"
import { DatabaseManagerService } from "./database-manager.service"
import { DatabaseController } from "./database.controller"
import { DatabaseMigrationService } from "./database-migration.service"

@Module({
  imports: [
    // Import DevCycle module for feature flags
    DevCycleModule,
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule, DevCycleModule],
      useFactory: async (configService: ConfigService) => {
        const isProduction = configService.get("NODE_ENV") === "production"
        const usePostgres = configService.get("DATABASE_TYPE") === "postgres"

        const baseConfig = {
          entities: [User, Product, CartItem, Order, OrderItem],
          synchronize: !isProduction, // Don't auto-sync in production
          logging: !isProduction,
          retryAttempts: 3,
          retryDelay: 3000,
        }

        if (usePostgres) {
          console.log("🐘 Using PostgreSQL database (startup configuration)")
          return {
            ...baseConfig,
            type: "postgres" as const,
            url: configService.get("POSTGRES_URL"),
            ssl: isProduction ? { rejectUnauthorized: false } : false,
          }
        }

        console.log("📂 Using SQLite database (startup configuration)")
        return {
          ...baseConfig,
          type: "sqlite" as const,
          database: configService.get<string>(
            "DATABASE_URL",
            "./database.sqlite"
          ),
        }
      },
      inject: [ConfigService],
    }),
  ],
  controllers: [DatabaseController],
  providers: [
    DatabaseConfigService,
    DatabaseService,
    DatabaseManagerService,
    DatabaseMigrationService,
  ],
  exports: [
    DatabaseConfigService,
    DatabaseService,
    DatabaseManagerService,
    DatabaseMigrationService,
  ],
})
export class DatabaseModule {}
