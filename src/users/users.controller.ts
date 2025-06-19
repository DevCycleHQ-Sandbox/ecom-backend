import { Controller, Get, Request, UseGuards } from "@nestjs/common"
import { UsersService } from "./users.service"
import { JwtAuthGuard } from "../auth/guards/jwt-auth.guard"

@Controller("users")
@UseGuards(JwtAuthGuard)
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Get("profile")
  getProfile(@Request() req) {
    return this.usersService.getProfile(req.user.id)
  }

  @Get("stats")
  getStats(@Request() req) {
    return this.usersService.getStats(req.user.id)
  }
}
