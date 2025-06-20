import { Injectable, NotFoundException } from "@nestjs/common"
import { Order } from "../entities/order.entity"
import { OrderItem } from "../entities/order-item.entity"
import { CreateOrderDto } from "./dto/create-order.dto"
import { DualDatabaseService } from "../database/dual-database.service"

@Injectable()
export class OrdersService {
  constructor(private dualDatabaseService: DualDatabaseService) {}

  async create(userId: string, createOrderDto: CreateOrderDto): Promise<Order> {
    const {
      items,
      shipping_address,
      card_number,
      shipping_cost = 0,
      tax_amount = 0,
    } = createOrderDto

    // Calculate subtotal from items
    const subtotal = items.reduce(
      (sum, item) => sum + item.price * item.quantity,
      0
    )

    // Calculate total amount including shipping and tax
    const total_amount = subtotal + shipping_cost + tax_amount

    // Create order
    const order = new Order()
    Object.assign(order, {
      user_id: userId,
      total_amount,
      shipping_address,
      card_number,
      status: "pending",
    })

    const savedOrder = await this.dualDatabaseService.dualSave(Order, order)

    // Create order items
    for (const item of items) {
      const orderItem = new OrderItem()
      Object.assign(orderItem, {
        order_id: savedOrder.id,
        product_id: item.product_id,
        quantity: item.quantity,
        price: item.price,
      })
      await this.dualDatabaseService.dualSave(OrderItem, orderItem)
    }

    // Return order with items
    return await this.dualDatabaseService.findOne(userId, Order, {
      where: { id: savedOrder.id },
      relations: ["orderItems", "orderItems.product"],
    })
  }

  async findAll(userId: string): Promise<Order[]> {
    return await this.dualDatabaseService.findMany(userId, Order, {
      where: { user_id: userId },
      relations: ["orderItems", "orderItems.product"],
      order: { created_at: "DESC" },
    })
  }

  async findOne(id: string, userId: string): Promise<Order> {
    const order = await this.dualDatabaseService.findOne(userId, Order, {
      where: { id, user_id: userId },
      relations: ["orderItems", "orderItems.product"],
    })

    if (!order) {
      throw new NotFoundException("Order not found")
    }

    return order
  }

  async updateStatus(
    id: string,
    status: string,
    userId: string = "system"
  ): Promise<Order> {
    const order = await this.dualDatabaseService.findOne(userId, Order, {
      where: { id },
    })
    if (!order) {
      throw new NotFoundException("Order not found")
    }

    await this.dualDatabaseService.dualUpdate(Order, id, { status })
    return await this.findOne(id, userId)
  }
}
