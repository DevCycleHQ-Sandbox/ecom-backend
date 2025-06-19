import { CartService } from "./cart.service";
import { AddToCartDto } from "./dto/add-to-cart.dto";
import { UpdateCartItemDto } from "./dto/update-cart-item.dto";
export declare class CartController {
    private readonly cartService;
    constructor(cartService: CartService);
    getCart(req: any): Promise<import("../entities/cart-item.entity").CartItem[]>;
    addToCart(req: any, addToCartDto: AddToCartDto): Promise<import("../entities/cart-item.entity").CartItem>;
    updateCartItem(req: any, id: string, updateCartItemDto: UpdateCartItemDto): Promise<import("../entities/cart-item.entity").CartItem>;
    removeFromCart(req: any, id: string): Promise<void>;
    clearCart(req: any): Promise<void>;
}
