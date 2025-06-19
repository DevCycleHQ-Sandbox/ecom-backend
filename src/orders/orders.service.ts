import { Injectable, NotFoundException } from "@nestjs/common"
import { InjectRepository } from "@nestjs/typeorm"
import { Repository } from "typeorm"
import { Order } from "../entities/order.entity"
import { OrderItem } from "../entities/order-item.entity"
import { CreateOrderDto } from "./dto/create-order.dto"

@Injectable()
export class OrdersService {
  constructor(
    @InjectRepository(Order)
    private orderRepository: Repository<Order>,
    @InjectRepository(OrderItem)
    private orderItemRepository: Repository<OrderItem>
  ) {}

  async create(userId: string, createOrderDto: CreateOrderDto): Promise<Order> {
    const {
      shipping_address,
      card_number,
      items,
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
    const order = this.orderRepository.create({
      user_id: userId,
      total_amount,
      shipping_address,
      card_number,
      status: "pending",
    })

    const savedOrder = await this.orderRepository.save(order)

    // Create order items
    const orderItems = items.map((item) =>
      this.orderItemRepository.create({
        order_id: savedOrder.id,
        product_id: item.product_id,
        quantity: item.quantity,
        price: item.price,
      })
    )

    await this.orderItemRepository.save(orderItems)

    return await this.orderRepository.findOne({
      where: { id: savedOrder.id },
      relations: ["items", "items.product"],
    })
  }

  async findAllByUser(userId: string): Promise<Order[]> {
    return await this.orderRepository.find({
      where: { user_id: userId },
      relations: ["items", "items.product"],
      order: { created_at: "DESC" },
    })
  }

  async findOne(id: string, userId: string): Promise<Order> {
    const order = await this.orderRepository.findOne({
      where: { id, user_id: userId },
      relations: ["items", "items.product"],
    })

    if (!order) {
      throw new NotFoundException("Order not found")
    }

    return order
  }

  async updateStatus(
    id: string,
    status: "pending" | "processing" | "shipped" | "delivered" | "cancelled"
  ): Promise<Order> {
    await this.orderRepository.update(id, { status })
    const updatedOrder = await this.orderRepository.findOne({
      where: { id },
      relations: ["items", "items.product"],
    })

    if (!updatedOrder) {
      throw new NotFoundException("Order not found")
    }

    return updatedOrder
  }
}
