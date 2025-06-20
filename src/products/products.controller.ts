import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  UseGuards,
} from "@nestjs/common"
import { ApiResponse, ApiTags } from "@nestjs/swagger"
import { ProductsService } from "./products.service"
import { CreateProductDto } from "./dto/create-product.dto"
import { UpdateProductDto } from "./dto/update-product.dto"
import { JwtAuthGuard } from "../auth/guards/jwt-auth.guard"
import { RolesGuard } from "../auth/guards/roles.guard"
import { Roles } from "../auth/decorators/roles.decorator"
import { Username } from "../auth/decorators/username.decorator"

@ApiTags("products")
@Controller("products")
export class ProductsController {
  constructor(private readonly productsService: ProductsService) {}

  @Post()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles("admin")
  create(@Body() createProductDto: CreateProductDto) {
    return this.productsService.create(createProductDto)
  }

  @Get()
  @ApiResponse({
    status: 200,
    description: "Returns all products successfully",
  })
  @ApiResponse({
    status: 500,
    description: "Internal server error",
  })
  findAll(@Username() username: string) {
    // Use username for feature flag targeting
    return this.productsService.findAll(username)
  }

  @Get(":id")
  findOne(@Param("id") id: string, @Username() username: string) {
    // Use username for feature flag targeting
    return this.productsService.findOne(id, username)
  }

  @Patch(":id")
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles("admin")
  update(
    @Param("id") id: string,
    @Body() updateProductDto: UpdateProductDto,
    @Username() username: string
  ) {
    // Use username for feature flag targeting
    return this.productsService.update(id, updateProductDto, username)
  }

  @Delete(":id")
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles("admin")
  remove(@Param("id") id: string) {
    return this.productsService.remove(id)
  }
}
