import { CartItem } from "./cart-item.entity";
import { OrderItem } from "./order-item.entity";
export declare class Product {
    id: string;
    name: string;
    description: string;
    price: number;
    image_url: string;
    category: string;
    stock_quantity: number;
    created_at: Date;
    updated_at: Date;
    cartItems: CartItem[];
    orderItems: OrderItem[];
}
