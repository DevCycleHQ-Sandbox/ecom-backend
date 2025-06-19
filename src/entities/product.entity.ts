import {
  Column,
  CreateDateColumn,
  Entity,
  OneToMany,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from "typeorm"
import { CartItem } from "./cart-item.entity"
import { OrderItem } from "./order-item.entity"

@Entity("products")
export class Product {
  @PrimaryGeneratedColumn("uuid")
  id: string

  @Column()
  name: string

  @Column("text")
  description: string

  @Column("decimal", { precision: 10, scale: 2 })
  price: number

  @Column()
  image_url: string

  @Column()
  category: string

  @Column({ default: 0 })
  stock_quantity: number

  @CreateDateColumn()
  created_at: Date

  @UpdateDateColumn()
  updated_at: Date

  @OneToMany(() => CartItem, (cartItem) => cartItem.product)
  cartItems: CartItem[]

  @OneToMany(() => OrderItem, (orderItem) => orderItem.product)
  orderItems: OrderItem[]
}
