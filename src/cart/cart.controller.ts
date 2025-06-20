import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  UseGuards,
} from "@nestjs/common"
import { CartService } from "./cart.service"
import { AddToCartDto } from "./dto/add-to-cart.dto"
import { UpdateCartItemDto } from "./dto/update-cart-item.dto"
import { JwtAuthGuard } from "../auth/guards/jwt-auth.guard"
import { Username } from "../auth/decorators/username.decorator"

@Controller("cart")
@UseGuards(JwtAuthGuard)
export class CartController {
  constructor(private readonly cartService: CartService) {}

  @Get()
  getCart(@Username() username: string) {
    return this.cartService.getCartItems(username)
  }

  @Post()
  addToCart(@Username() username: string, @Body() addToCartDto: AddToCartDto) {
    return this.cartService.addToCart(username, addToCartDto)
  }

  @Patch(":id")
  updateCartItem(
    @Username() username: string,
    @Param("id") id: string,
    @Body() updateCartItemDto: UpdateCartItemDto
  ) {
    return this.cartService.updateCartItem(username, id, updateCartItemDto)
  }

  @Delete(":id")
  removeFromCart(@Username() username: string, @Param("id") id: string) {
    return this.cartService.removeCartItem(username, id)
  }

  @Delete()
  clearCart(@Username() username: string) {
    return this.cartService.clearCart(username)
  }
}
