import { Repository } from "typeorm";
import { User } from "../entities/user.entity";
import { Order } from "../entities/order.entity";
export declare class UsersService {
    private userRepository;
    private orderRepository;
    constructor(userRepository: Repository<User>, orderRepository: Repository<Order>);
    getProfile(userId: string): Promise<User>;
    getStats(userId: string): Promise<{
        totalOrders: number;
        totalSpent: number;
        memberSince: Date;
    }>;
}
