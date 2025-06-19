import { Injectable, NotFoundException } from "@nestjs/common"
import { InjectRepository } from "@nestjs/typeorm"
import { Repository } from "typeorm"
import { CartItem } from "../entities/cart-item.entity"
import { Product } from "../entities/product.entity"

@Injectable()
export class CartService {
  constructor(
    @InjectRepository(CartItem)
    private cartRepository: Repository<CartItem>,
    @InjectRepository(Product)
    private productRepository: Repository<Product>
  ) {}

  async getCartItems(userId: string): Promise<CartItem[]> {
    return await this.cartRepository.find({
      where: { user_id: userId },
      relations: ["product"],
    })
  }

  async addToCart(
    userId: string,
    productId: string,
    quantity: number
  ): Promise<CartItem> {
    // Check if product exists
    const product = await this.productRepository.findOne({
      where: { id: productId },
    })
    if (!product) {
      throw new NotFoundException("Product not found")
    }

    // Check if item already exists in cart
    const existingItem = await this.cartRepository.findOne({
      where: { user_id: userId, product_id: productId },
    })

    if (existingItem) {
      // Update quantity
      existingItem.quantity += quantity
      return await this.cartRepository.save(existingItem)
    }

    // Create new cart item
    const cartItem = this.cartRepository.create({
      user_id: userId,
      product_id: productId,
      quantity,
    })

    return await this.cartRepository.save(cartItem)
  }

  async updateCartItem(
    userId: string,
    cartItemId: string,
    quantity: number
  ): Promise<CartItem> {
    const cartItem = await this.cartRepository.findOne({
      where: { id: cartItemId, user_id: userId },
    })

    if (!cartItem) {
      throw new NotFoundException("Cart item not found")
    }

    cartItem.quantity = quantity
    return await this.cartRepository.save(cartItem)
  }

  async removeFromCart(userId: string, cartItemId: string): Promise<void> {
    const result = await this.cartRepository.delete({
      id: cartItemId,
      user_id: userId,
    })

    if (result.affected === 0) {
      throw new NotFoundException("Cart item not found")
    }
  }

  async clearCart(userId: string): Promise<void> {
    await this.cartRepository.delete({ user_id: userId })
  }
}
