import {
  Body,
  Controller,
  Get,
  Param,
  Patch,
  Post,
  Request,
  UseGuards,
} from "@nestjs/common"
import { OrdersService } from "./orders.service"
import { CreateOrderDto } from "./dto/create-order.dto"
import { JwtAuthGuard } from "../auth/guards/jwt-auth.guard"
import { RolesGuard } from "../auth/guards/roles.guard"
import { Roles } from "../auth/decorators/roles.decorator"

@Controller("orders")
@UseGuards(JwtAuthGuard)
export class OrdersController {
  constructor(private readonly ordersService: OrdersService) {}

  @Post()
  create(@Request() req, @Body() createOrderDto: CreateOrderDto) {
    return this.ordersService.create(req.user.id, createOrderDto)
  }

  @Get()
  findAll(@Request() req) {
    return this.ordersService.findAll(req.user.id)
  }

  @Get(":id")
  findOne(@Request() req, @Param("id") id: string) {
    return this.ordersService.findOne(id, req.user.id)
  }

  @Patch(":id/status")
  @UseGuards(RolesGuard)
  @Roles("admin")
  updateStatus(
    @Request() req,
    @Param("id") id: string,
    @Body("status") status: string
  ) {
    return this.ordersService.updateStatus(id, status, req.user?.id || "system")
  }
}
