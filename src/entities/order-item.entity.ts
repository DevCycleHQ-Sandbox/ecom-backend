import {
  Column,
  CreateDateColumn,
  Entity,
  JoinColumn,
  ManyToOne,
  PrimaryGeneratedColumn,
} from "typeorm"
import { Order } from "./order.entity"
import { Product } from "./product.entity"

@Entity("order_items")
export class OrderItem {
  @PrimaryGeneratedColumn("uuid")
  id: string

  @Column()
  order_id: string

  @Column()
  product_id: string

  @Column()
  quantity: number

  @Column("decimal", { precision: 10, scale: 2 })
  price: number

  @CreateDateColumn()
  created_at: Date

  @ManyToOne(() => Order, (order) => order.items)
  @JoinColumn({ name: "order_id" })
  order: Order

  @ManyToOne(() => Product, (product) => product.orderItems)
  @JoinColumn({ name: "product_id" })
  product: Product
}
