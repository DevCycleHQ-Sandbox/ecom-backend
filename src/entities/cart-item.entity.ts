import {
  Column,
  CreateDateColumn,
  Entity,
  JoinColumn,
  ManyToOne,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from "typeorm"
import { User } from "./user.entity"
import { Product } from "./product.entity"

@Entity("cart_items")
export class CartItem {
  @PrimaryGeneratedColumn("uuid")
  id: string

  @Column()
  user_id: string

  @Column()
  product_id: string

  @Column({ default: 1 })
  quantity: number

  @CreateDateColumn()
  created_at: Date

  @UpdateDateColumn()
  updated_at: Date

  @ManyToOne(() => User, (user) => user.cartItems)
  @JoinColumn({ name: "user_id" })
  user: User

  @ManyToOne(() => Product, (product) => product.cartItems)
  @JoinColumn({ name: "product_id" })
  product: Product
}
