import { Repository } from "typeorm";
import { CartItem } from "../entities/cart-item.entity";
import { Product } from "../entities/product.entity";
export declare class CartService {
    private cartRepository;
    private productRepository;
    constructor(cartRepository: Repository<CartItem>, productRepository: Repository<Product>);
    getCartItems(userId: string): Promise<CartItem[]>;
    addToCart(userId: string, productId: string, quantity: number): Promise<CartItem>;
    updateCartItem(userId: string, cartItemId: string, quantity: number): Promise<CartItem>;
    removeFromCart(userId: string, cartItemId: string): Promise<void>;
    clearCart(userId: string): Promise<void>;
}
