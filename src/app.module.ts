import { Module } from "@nestjs/common"
import { ConfigModule } from "@nestjs/config"
import { AuthModule } from "./auth/auth.module"
import { ProductsModule } from "./products/products.module"
import { CartModule } from "./cart/cart.module"
import { OrdersModule } from "./orders/orders.module"
import { UsersModule } from "./users/users.module"
import { DatabaseModule } from "./database/database.module"
import { FeatureFlagModule } from "./feature-flags/feature-flag.module"
import { AdminModule } from "./admin/admin.module"

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: ".env",
    }),
    DatabaseModule,
    AuthModule,
    ProductsModule,
    CartModule,
    OrdersModule,
    UsersModule,
    FeatureFlagModule,
    AdminModule,
  ],
})
export class AppModule {}
