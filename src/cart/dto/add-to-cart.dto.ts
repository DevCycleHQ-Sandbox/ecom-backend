import { IsNotEmpty, IsNumber, IsPositive, IsString } from "class-validator"

export class AddToCartDto {
  @IsString()
  @IsNotEmpty()
  product_id: string

  @IsNumber()
  @IsPositive()
  quantity: number
}
