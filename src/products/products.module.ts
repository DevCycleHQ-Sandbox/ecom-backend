import { Module } from "@nestjs/common"
import { ProductsService } from "./products.service"
import { ProductsController } from "./products.controller"
import { AuthModule } from "../auth/auth.module"
import { DatabaseModule } from "../database/database.module"
import { FeatureFlagModule } from "../feature-flags/feature-flag.module"

@Module({
  imports: [DatabaseModule, AuthModule, FeatureFlagModule],
  controllers: [ProductsController],
  providers: [ProductsService],
  exports: [ProductsService],
})
export class ProductsModule {}
