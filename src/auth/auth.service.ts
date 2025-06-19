import {
  ConflictException,
  Injectable,
  UnauthorizedException,
} from "@nestjs/common"
import { JwtService } from "@nestjs/jwt"
import { InjectRepository } from "@nestjs/typeorm"
import { Repository } from "typeorm"
import * as bcrypt from "bcryptjs"
import { User } from "../entities/user.entity"
import { LoginDto } from "./dto/login.dto"
import { RegisterDto } from "./dto/register.dto"

@Injectable()
export class AuthService {
  constructor(
    @InjectRepository(User)
    private userRepository: Repository<User>,
    private jwtService: JwtService
  ) {}

  async register(registerDto: RegisterDto) {
    const { username, email, password } = registerDto

    // Check if user already exists
    const existingUser = await this.userRepository.findOne({
      where: [{ username }, { email }],
    })

    if (existingUser) {
      throw new ConflictException("Username or email already exists")
    }

    // Hash password
    const hashedPassword = await bcrypt.hash(password, 12)

    // Create user
    const user = this.userRepository.create({
      username,
      email,
      password: hashedPassword,
      role: "user",
    })

    await this.userRepository.save(user)

    // Generate JWT token
    const payload = {
      userId: user.id,
      username: user.username,
      role: user.role,
    }
    const token = this.jwtService.sign(payload)

    return {
      message: "User registered successfully",
      token,
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
        role: user.role,
      },
    }
  }

  async login(loginDto: LoginDto) {
    const { username, password } = loginDto

    // Find user
    const user = await this.userRepository.findOne({ where: { username } })
    if (!user) {
      throw new UnauthorizedException("Invalid credentials")
    }

    // Check password
    const isPasswordValid = await bcrypt.compare(password, user.password)
    if (!isPasswordValid) {
      throw new UnauthorizedException("Invalid credentials")
    }

    // Generate JWT token
    const payload = {
      userId: user.id,
      username: user.username,
      role: user.role,
    }
    const token = this.jwtService.sign(payload)

    return {
      message: "Login successful",
      token,
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
        role: user.role,
      },
    }
  }

  async validateUser(userId: string): Promise<User> {
    const user = await this.userRepository.findOne({ where: { id: userId } })
    if (!user) {
      throw new UnauthorizedException("User not found")
    }
    return user
  }
}
