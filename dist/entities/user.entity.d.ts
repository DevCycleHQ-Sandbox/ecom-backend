import { CartItem } from "./cart-item.entity";
import { Order } from "./order.entity";
export declare class User {
    id: string;
    username: string;
    email: string;
    password: string;
    role: "admin" | "user";
    created_at: Date;
    updated_at: Date;
    cartItems: CartItem[];
    orders: Order[];
}
