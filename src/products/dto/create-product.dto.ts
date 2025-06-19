import {
  IsNotEmpty,
  IsNumber,
  IsPositive,
  IsString,
  Min,
} from "class-validator"

export class CreateProductDto {
  @IsString()
  @IsNotEmpty()
  name: string

  @IsString()
  @IsNotEmpty()
  description: string

  @IsNumber()
  @IsPositive()
  price: number

  @IsString()
  @IsNotEmpty()
  image_url: string

  @IsString()
  @IsNotEmpty()
  category: string

  @IsNumber()
  @Min(0)
  stock_quantity: number
}
