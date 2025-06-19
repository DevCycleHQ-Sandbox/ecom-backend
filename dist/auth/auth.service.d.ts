import { JwtService } from "@nestjs/jwt";
import { Repository } from "typeorm";
import { User } from "../entities/user.entity";
import { LoginDto } from "./dto/login.dto";
import { RegisterDto } from "./dto/register.dto";
export declare class AuthService {
    private userRepository;
    private jwtService;
    constructor(userRepository: Repository<User>, jwtService: JwtService);
    register(registerDto: RegisterDto): Promise<{
        message: string;
        token: string;
        user: {
            id: string;
            username: string;
            email: string;
            role: "user" | "admin";
        };
    }>;
    login(loginDto: LoginDto): Promise<{
        message: string;
        token: string;
        user: {
            id: string;
            username: string;
            email: string;
            role: "user" | "admin";
        };
    }>;
    validateUser(userId: string): Promise<User>;
}
