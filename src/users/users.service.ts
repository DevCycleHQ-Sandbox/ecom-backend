import { Injectable, NotFoundException } from "@nestjs/common"
import { User } from "../entities/user.entity"
import { Order } from "../entities/order.entity"
import { DualDatabaseService } from "../database/dual-database.service"
import { omit } from "lodash"

@Injectable()
export class UsersService {
  constructor(private dualDatabaseService: DualDatabaseService) {}

  async findAll(userId: string = "system"): Promise<User[]> {
    return await this.dualDatabaseService.findMany(userId, User)
  }

  async findOne(id: string, userId: string = "system"): Promise<User> {
    const user = await this.dualDatabaseService.findOne(userId, User, {
      where: { id },
      relations: ["orders"],
    })
    if (!user) {
      throw new NotFoundException("User not found")
    }
    return user
  }

  async findByEmail(
    email: string,
    userId: string = "system"
  ): Promise<User | null> {
    return await this.dualDatabaseService.findOne(userId, User, {
      where: { email },
    })
  }

  async create(userData: Partial<User>): Promise<User> {
    const user = new User()
    Object.assign(user, userData)
    return await this.dualDatabaseService.dualSave(User, user)
  }

  async update(
    id: string,
    userData: Partial<User>,
    userId: string = "system"
  ): Promise<User> {
    await this.dualDatabaseService.dualUpdate(User, id, userData)
    return await this.findOne(id, userId)
  }

  async findUserOrders(
    userId: string,
    requestingUserId: string = "system"
  ): Promise<Order[]> {
    return await this.dualDatabaseService.findMany(requestingUserId, Order, {
      where: { user: { id: userId } },
      relations: ["orderItems", "orderItems.product"],
    })
  }

  async getProfile(userId: string): Promise<User> {
    const user = await this.dualDatabaseService.findOne(userId, User, {
      where: { id: userId },
    })
    if (!user) {
      throw new NotFoundException("User not found")
    }

    // Remove password from response
    const userProfile = omit(user, ["password"])
    return userProfile as User
  }

  async getStats(userId: string) {
    const user = await this.dualDatabaseService.findOne(userId, User, {
      where: { id: userId },
    })
    if (!user) {
      throw new NotFoundException("User not found")
    }

    const orders = await this.dualDatabaseService.findMany(userId, Order, {
      where: { user_id: userId },
    })

    const totalSpent = orders.reduce(
      (sum, order) => sum + Number(order.total_amount),
      0
    )

    return {
      totalOrders: orders.length,
      totalSpent,
      memberSince: user.created_at,
    }
  }
}
