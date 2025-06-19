import { IsArray, IsNotEmpty, IsString, ValidateNested } from "class-validator"
import { Type } from "class-transformer"

class OrderItemDto {
  @IsString()
  @IsNotEmpty()
  product_id: string

  @IsNotEmpty()
  quantity: number

  @IsNotEmpty()
  price: number
}

export class CreateOrderDto {
  @IsString()
  @IsNotEmpty()
  shipping_address: string

  @IsString()
  @IsNotEmpty()
  card_number: string

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => OrderItemDto)
  items: OrderItemDto[]
}
