import {
  Column,
  CreateDateColumn,
  Entity,
  OneToMany,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from "typeorm"
import { CartItem } from "./cart-item.entity"
import { Order } from "./order.entity"

@Entity("users")
export class User {
  @PrimaryGeneratedColumn("uuid")
  id: string

  @Column({ unique: true })
  username: string

  @Column({ unique: true })
  email: string

  @Column()
  password: string

  @Column({ default: "user" })
  role: "admin" | "user"

  @CreateDateColumn()
  created_at: Date

  @UpdateDateColumn()
  updated_at: Date

  @OneToMany(() => CartItem, (cartItem) => cartItem.user)
  cartItems: CartItem[]

  @OneToMany(() => Order, (order) => order.user)
  orders: Order[]
}
