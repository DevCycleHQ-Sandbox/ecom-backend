import { Repository } from "typeorm";
import { Order } from "../entities/order.entity";
import { OrderItem } from "../entities/order-item.entity";
import { CreateOrderDto } from "./dto/create-order.dto";
export declare class OrdersService {
    private orderRepository;
    private orderItemRepository;
    constructor(orderRepository: Repository<Order>, orderItemRepository: Repository<OrderItem>);
    create(userId: string, createOrderDto: CreateOrderDto): Promise<Order>;
    findAllByUser(userId: string): Promise<Order[]>;
    findOne(id: string, userId: string): Promise<Order>;
    updateStatus(id: string, status: "pending" | "processing" | "shipped" | "delivered" | "cancelled"): Promise<Order>;
}
