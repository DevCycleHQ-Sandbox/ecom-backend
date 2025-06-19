import { Injectable, NotFoundException } from "@nestjs/common"
import { InjectRepository } from "@nestjs/typeorm"
import { Repository } from "typeorm"
import { User } from "../entities/user.entity"
import { Order } from "../entities/order.entity"
import { omit } from "lodash"

@Injectable()
export class UsersService {
  constructor(
    @InjectRepository(User)
    private userRepository: Repository<User>,
    @InjectRepository(Order)
    private orderRepository: Repository<Order>
  ) {}

  async getProfile(userId: string): Promise<User> {
    const user = await this.userRepository.findOne({ where: { id: userId } })
    if (!user) {
      throw new NotFoundException("User not found")
    }

    const userProfile = omit(user, ["password"])
    return userProfile as User
  }

  async getStats(userId: string) {
    const user = await this.userRepository.findOne({ where: { id: userId } })
    if (!user) {
      throw new NotFoundException("User not found")
    }

    const orders = await this.orderRepository.find({
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
