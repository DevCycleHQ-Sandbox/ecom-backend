import { Module } from "@nestjs/common"
import { TypeOrmModule } from "@nestjs/typeorm"
import { CartItem } from "../entities/cart-item.entity"
import { Product } from "../entities/product.entity"
import { CartService } from "./cart.service"
import { CartController } from "./cart.controller"
import { AuthModule } from "../auth/auth.module"

@Module({
  imports: [TypeOrmModule.forFeature([CartItem, Product]), AuthModule],
  controllers: [CartController],
  providers: [CartService],
  exports: [CartService],
})
export class CartModule {}
