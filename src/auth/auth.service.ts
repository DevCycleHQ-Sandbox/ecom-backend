import { Injectable, UnauthorizedException } from "@nestjs/common"
import { JwtService } from "@nestjs/jwt"
import * as bcrypt from "bcryptjs"
import { User } from "../entities/user.entity"
import { LoginDto } from "./dto/login.dto"
import { RegisterDto } from "./dto/register.dto"
import { DualDatabaseService } from "../database/dual-database.service"
import { omit } from "lodash"

@Injectable()
export class AuthService {
  constructor(
    private dualDatabaseService: DualDatabaseService,
    private jwtService: JwtService
  ) {}

  async register(
    registerDto: RegisterDto
  ): Promise<{ user: User; token: string }> {
    const { username, email, password } = registerDto

    // Check if user already exists
    const existingUser = await this.dualDatabaseService.findOne(
      "system",
      User,
      {
        where: [{ username }, { email }],
      }
    )
    if (existingUser) {
      throw new UnauthorizedException("User already exists")
    }

    // Hash password
    const hashedPassword = await bcrypt.hash(password, 10)

    // Create user
    const user = new User()
    Object.assign(user, {
      username,
      email,
      password: hashedPassword,
      role: "customer",
    })

    const savedUser = await this.dualDatabaseService.dualSave(User, user)

    // Generate JWT token
    const token = this.jwtService.sign({
      userId: savedUser.id,
      username: savedUser.username,
      email: savedUser.email,
      role: savedUser.role,
    })

    // Remove password from response
    const userWithoutPassword = omit(savedUser, ["password"])

    return { user: userWithoutPassword as User, token }
  }

  async login(loginDto: LoginDto): Promise<{ user: User; token: string }> {
    const { username, password } = loginDto

    // Find user by username
    const user = await this.dualDatabaseService.findOne("system", User, {
      where: { username },
    })
    if (!user) {
      throw new UnauthorizedException("Invalid credentials")
    }

    // Check password
    const isPasswordValid = await bcrypt.compare(password, user.password)
    if (!isPasswordValid) {
      throw new UnauthorizedException("Invalid credentials")
    }

    // Generate JWT token
    const token = this.jwtService.sign({
      userId: user.id,
      username: user.username,
      email: user.email,
      role: user.role,
    })

    // Remove password from response
    const userWithoutPassword = omit(user, ["password"])

    return { user: userWithoutPassword as User, token }
  }

  async validateUser(userId: string): Promise<User | null> {
    return await this.dualDatabaseService.findOne("system", User, {
      where: { id: userId },
    })
  }
}
