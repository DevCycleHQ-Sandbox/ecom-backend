import { Order } from "./order.entity";
import { Product } from "./product.entity";
export declare class OrderItem {
    id: string;
    order_id: string;
    product_id: string;
    quantity: number;
    price: number;
    created_at: Date;
    order: Order;
    product: Product;
}
