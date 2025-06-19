import {
  Column,
  CreateDateColumn,
  Entity,
  JoinColumn,
  ManyToOne,
  OneToMany,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from "typeorm"
import { User } from "./user.entity"
import { OrderItem } from "./order-item.entity"

@Entity("orders")
export class Order {
  @PrimaryGeneratedColumn("uuid")
  id: string

  @Column()
  user_id: string

  @Column("decimal", { precision: 10, scale: 2 })
  total_amount: number

  @Column({ default: "pending" })
  status: "pending" | "processing" | "shipped" | "delivered" | "cancelled"

  @Column()
  shipping_address: string

  @Column()
  card_number: string

  @CreateDateColumn()
  created_at: Date

  @UpdateDateColumn()
  updated_at: Date

  @ManyToOne(() => User, (user) => user.orders)
  @JoinColumn({ name: "user_id" })
  user: User

  @OneToMany(() => OrderItem, (orderItem) => orderItem.order)
  items: OrderItem[]
}
