import { User } from "./user.entity";
import { Product } from "./product.entity";
export declare class CartItem {
    id: string;
    user_id: string;
    product_id: string;
    quantity: number;
    created_at: Date;
    updated_at: Date;
    user: User;
    product: Product;
}
