import { Module } from "@nestjs/common"
import { CartService } from "./cart.service"
import { CartController } from "./cart.controller"
import { AuthModule } from "../auth/auth.module"
import { DatabaseModule } from "../database/database.module"

@Module({
  imports: [DatabaseModule, AuthModule],
  controllers: [CartController],
  providers: [CartService],
  exports: [CartService],
})
export class CartModule {}
