import { Module } from "@nestjs/common"
import { TypeOrmModule } from "@nestjs/typeorm"
import { ConfigModule, ConfigService } from "@nestjs/config"
import { User } from "../entities/user.entity"
import { Product } from "../entities/product.entity"
import { CartItem } from "../entities/cart-item.entity"
import { Order } from "../entities/order.entity"
import { OrderItem } from "../entities/order-item.entity"
import { FeatureFlagModule } from "../feature-flags/feature-flag.module"
import { DualDatabaseService } from "./dual-database.service"
import { SyncService } from "./sync.service"

@Module({
  imports: [
    FeatureFlagModule,
    // SQLite Connection
    TypeOrmModule.forRootAsync({
      name: "sqlite",
      imports: [ConfigModule],
      useFactory: (configService: ConfigService) => {
        const isProduction = configService.get("NODE_ENV") === "production"

        console.log("🗃️  Setting up SQLite database connection")
        return {
          type: "sqlite" as const,
          database: configService.get<string>(
            "DATABASE_URL",
            "./database.sqlite"
          ),
          entities: [User, Product, CartItem, Order, OrderItem],
          synchronize: !isProduction,
          logging: !isProduction,
        }
      },
      inject: [ConfigService],
    }),
    // PostgreSQL Connection
    TypeOrmModule.forRootAsync({
      name: "postgres",
      imports: [ConfigModule],
      useFactory: (configService: ConfigService) => {
        const isProduction = configService.get("NODE_ENV") === "production"
        const postgresUrl = configService.get("POSTGRES_URL")

        console.log("🐘 Setting up PostgreSQL database connection")
        return {
          type: "postgres" as const,
          url: postgresUrl,
          entities: [User, Product, CartItem, Order, OrderItem],
          synchronize: !isProduction,
          logging: !isProduction,
          // Handle case where POSTGRES_URL might not be set
          ...(postgresUrl
            ? {}
            : {
                host: "localhost",
                port: 5432,
                username: "test",
                password: "test",
                database: "shopper_test",
              }),
        }
      },
      inject: [ConfigService],
    }),
  ],
  providers: [DualDatabaseService, SyncService],
  exports: [DualDatabaseService, SyncService],
})
export class DatabaseModule {}
