import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  Request,
  UseGuards,
} from "@nestjs/common"
import { CartService } from "./cart.service"
import { AddToCartDto } from "./dto/add-to-cart.dto"
import { UpdateCartItemDto } from "./dto/update-cart-item.dto"
import { JwtAuthGuard } from "../auth/guards/jwt-auth.guard"

@Controller("cart")
@UseGuards(JwtAuthGuard)
export class CartController {
  constructor(private readonly cartService: CartService) {}

  @Get()
  getCart(@Request() req) {
    return this.cartService.getCartItems(req.user.id)
  }

  @Post()
  addToCart(@Request() req, @Body() addToCartDto: AddToCartDto) {
    return this.cartService.addToCart(
      req.user.id,
      addToCartDto.product_id,
      addToCartDto.quantity
    )
  }

  @Patch(":id")
  updateCartItem(
    @Request() req,
    @Param("id") id: string,
    @Body() updateCartItemDto: UpdateCartItemDto
  ) {
    return this.cartService.updateCartItem(
      req.user.id,
      id,
      updateCartItemDto.quantity
    )
  }

  @Delete(":id")
  removeFromCart(@Request() req, @Param("id") id: string) {
    return this.cartService.removeFromCart(req.user.id, id)
  }

  @Delete()
  clearCart(@Request() req) {
    return this.cartService.clearCart(req.user.id)
  }
}
