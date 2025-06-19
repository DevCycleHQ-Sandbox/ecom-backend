import { OrdersService } from "./orders.service";
import { CreateOrderDto } from "./dto/create-order.dto";
export declare class OrdersController {
    private readonly ordersService;
    constructor(ordersService: OrdersService);
    create(req: any, createOrderDto: CreateOrderDto): Promise<import("../entities/order.entity").Order>;
    findAll(req: any): Promise<import("../entities/order.entity").Order[]>;
    findOne(req: any, id: string): Promise<import("../entities/order.entity").Order>;
    updateStatus(id: string, status: "pending" | "processing" | "shipped" | "delivered" | "cancelled"): Promise<import("../entities/order.entity").Order>;
}
