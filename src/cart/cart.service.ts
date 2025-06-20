import { Injectable, NotFoundException } from "@nestjs/common"
import { CartItem } from "../entities/cart-item.entity"
import { Product } from "../entities/product.entity"
import { User } from "../entities/user.entity"
import { AddToCartDto } from "./dto/add-to-cart.dto"
import { UpdateCartItemDto } from "./dto/update-cart-item.dto"
import { DualDatabaseService } from "../database/dual-database.service"

@Injectable()
export class CartService {
  constructor(private dualDatabaseService: DualDatabaseService) {}

  private async getUserIdFromUsername(username: string): Promise<string> {
    if (username === "anonymous") {
      return "anonymous"
    }

    const user = await this.dualDatabaseService.findOne(username, User, {
      where: { username },
    })

    if (!user) {
      throw new NotFoundException("User not found")
    }

    return user.id
  }

  async addToCart(
    username: string,
    addToCartDto: AddToCartDto
  ): Promise<CartItem> {
    const { product_id, quantity } = addToCartDto

    // Resolve username to user ID for business logic
    const userId = await this.getUserIdFromUsername(username)

    // Check if product exists (use username for feature flag targeting)
    const product = await this.dualDatabaseService.findOne(username, Product, {
      where: { id: product_id },
    })
    if (!product) {
      throw new NotFoundException("Product not found")
    }

    // Check if item already exists in cart (use username for feature flag targeting)
    const existingCartItem = await this.dualDatabaseService.findOne(
      username,
      CartItem,
      {
        where: { user_id: userId, product_id },
        relations: ["product"],
      }
    )

    if (existingCartItem) {
      // Update quantity
      const updatedQuantity = existingCartItem.quantity + quantity
      await this.dualDatabaseService.dualUpdate(CartItem, existingCartItem.id, {
        quantity: updatedQuantity,
      })
      return await this.dualDatabaseService.findOne(username, CartItem, {
        where: { id: existingCartItem.id },
        relations: ["product"],
      })
    } else {
      // Create new cart item
      const cartItem = new CartItem()
      Object.assign(cartItem, {
        user_id: userId,
        product_id,
        quantity,
        product,
      })
      return await this.dualDatabaseService.dualSave(CartItem, cartItem)
    }
  }

  async getCartItems(username: string): Promise<CartItem[]> {
    // Resolve username to user ID for business logic
    const userId = await this.getUserIdFromUsername(username)

    return await this.dualDatabaseService.findMany(username, CartItem, {
      where: { user_id: userId },
      relations: ["product"],
    })
  }

  async updateCartItem(
    username: string,
    id: string,
    updateCartItemDto: UpdateCartItemDto
  ): Promise<CartItem> {
    // Resolve username to user ID for business logic
    const userId = await this.getUserIdFromUsername(username)

    const cartItem = await this.dualDatabaseService.findOne(
      username,
      CartItem,
      {
        where: { id, user_id: userId },
        relations: ["product"],
      }
    )

    if (!cartItem) {
      throw new NotFoundException("Cart item not found")
    }

    await this.dualDatabaseService.dualUpdate(CartItem, id, updateCartItemDto)
    return await this.dualDatabaseService.findOne(username, CartItem, {
      where: { id },
      relations: ["product"],
    })
  }

  async removeCartItem(username: string, id: string): Promise<void> {
    // Resolve username to user ID for business logic
    const userId = await this.getUserIdFromUsername(username)

    const cartItem = await this.dualDatabaseService.findOne(
      username,
      CartItem,
      {
        where: { id, user_id: userId },
      }
    )

    if (!cartItem) {
      throw new NotFoundException("Cart item not found")
    }

    await this.dualDatabaseService.dualDelete(CartItem, id)
  }

  async clearCart(username: string): Promise<void> {
    // Resolve username to user ID for business logic
    const userId = await this.getUserIdFromUsername(username)

    const cartItems = await this.dualDatabaseService.findMany(
      username,
      CartItem,
      {
        where: { user_id: userId },
      }
    )

    for (const item of cartItems) {
      await this.dualDatabaseService.dualDelete(CartItem, item.id)
    }
  }
}
