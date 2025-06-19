import { Module } from "@nestjs/common"
import { TypeOrmModule } from "@nestjs/typeorm"
import { ConfigModule, ConfigService } from "@nestjs/config"
import { User } from "../entities/user.entity"
import { Product } from "../entities/product.entity"
import { CartItem } from "../entities/cart-item.entity"
import { Order } from "../entities/order.entity"
import { OrderItem } from "../entities/order-item.entity"

@Module({
  imports: [
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      useFactory: (configService: ConfigService) => {
        const isProduction = configService.get("NODE_ENV") === "production"
        const usePostgres = configService.get("DATABASE_TYPE") === "postgres"

        const baseConfig = {
          entities: [User, Product, CartItem, Order, OrderItem],
          synchronize: !isProduction, // Don't auto-sync in production
          logging: !isProduction,
        }

        if (usePostgres) {
          return {
            ...baseConfig,
            type: "postgres" as const,
            url: configService.get("POSTGRES_URL"),
          }
        }

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
})
export class DatabaseModule {}
