import { UsersService } from "./users.service";
export declare class UsersController {
    private readonly usersService;
    constructor(usersService: UsersService);
    getProfile(req: any): Promise<import("../entities/user.entity").User>;
    getStats(req: any): Promise<{
        totalOrders: number;
        totalSpent: number;
        memberSince: Date;
    }>;
}
