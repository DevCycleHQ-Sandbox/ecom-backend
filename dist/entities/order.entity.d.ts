import { User } from "./user.entity";
import { OrderItem } from "./order-item.entity";
export declare class Order {
    id: string;
    user_id: string;
    total_amount: number;
    status: "pending" | "processing" | "shipped" | "delivered" | "cancelled";
    shipping_address: string;
    card_number: string;
    created_at: Date;
    updated_at: Date;
    user: User;
    items: OrderItem[];
}
